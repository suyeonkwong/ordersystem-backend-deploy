package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductCreateDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.dto.ProductUpdateDto;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;

    @Value("${jwt.secretKeyAt}")
    private String secretKey;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public ProductService(ProductRepository productRepository, MemberRepository memberRepository, S3Client s3Client, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.s3Client = s3Client;
        this.stockInventoryService = stockInventoryService;
    }

    public Long addProduct(ProductCreateDto productCreateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        Product product = productRepository.save(productCreateDto.toEntity(member));

        if (productCreateDto.getProductImage() != null && !productCreateDto.getProductImage().isEmpty()) {
            String fileName = "product-" + product.getId() + "-productImage-" + productCreateDto.getProductImage().getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(productCreateDto.getProductImage().getContentType())
                    .build();

            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productCreateDto.getProductImage().getBytes()));
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImageUrl(imgUrl);
        }

        /// 상품등록시 redis에 재고세팅
        stockInventoryService.makeStockQuantity(product.getId(), product.getStockQuantity());
        return product.getId();
    }

    public Long productUpdate(Long targetId, ProductUpdateDto productUpdateDto) {
        Product target = productRepository.findById(targetId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));
        target.updateProduct(productUpdateDto);
        String targetFileName = target.getImagePath().substring(target.getImagePath().lastIndexOf("/")+1);

        if (productUpdateDto.getProductImage() != null &&!productUpdateDto.getProductImage().isEmpty()) {
            // 이미지 삭제 후 다시 저장
            s3Client.deleteObject(a -> a.bucket(bucket).key(targetFileName));

            String fileName = "product-" + target.getId() + "-productImage-" + productUpdateDto.getProductImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(productUpdateDto.getProductImage().getContentType())
                    .build();

            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productUpdateDto.getProductImage().getBytes()));
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            target.updateImageUrl(imgUrl);
        } else {
            target.updateImageUrl(null);
        }
        return target.getId();
    }

    public Page<ProductResDto> findAll(Pageable pageable, ProductSearchDto productSearchDto) {
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                // root: 엔티티의 소속을 접근하기 위한 객체, critetiaBuilder: 쿼리를 생성하기 위한 객체
                List<Predicate> predicateList = new ArrayList<>();

                if (productSearchDto.getCategory() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("category"), productSearchDto.getCategory()));
                }
                if (productSearchDto.getProductName() != null) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + productSearchDto.getProductName() + "%"));
                }

                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }

                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };

        Page<Product> productList = productRepository.findAll(specification, pageable);
        return productList.map(p -> ProductResDto.fromEntity(p));
    }

    public ProductResDto productDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품번호입니다."));
        return ProductResDto.fromEntity(product);
    }
}
