package com.example.ordersystem.product.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.product.dto.ProductCreateDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.entity.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Transactional
    public ProductResDto saveProduct(ProductCreateDto productCreateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));
        Product product = productRepository.save(productCreateDto.toEntity(member));

        /// image명 설정
        String fileName = "user-" + product.getId() + "-profileimage-" + productCreateDto.getMultipartFile().getOriginalFilename();
        /// 저장 객체 구성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(productCreateDto.getMultipartFile().getContentType())
                .build();

        if (productCreateDto.getMultipartFile() != null) {
            /// 이미지 업로드(byte 형태로)
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productCreateDto.getMultipartFile().getBytes()));
            } catch (IOException e) {
                /// checked -> unchecked로 바꿔 전체 rollback 되도록 예외차리
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

            /// 이미지 url 추출
            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImageUrl(imgUrl);
        }

        return ProductResDto.fromEntity(product);
    }

    @Transactional
    public Page<ProductResDto> getProductList(Pageable pageable, ProductSearchDto productSearchDto) {
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                /// Root : 엔티티의 속성을 접근하기 위한 객체, CriteriaBuilder : 쿼리를 생성하기 위한 객체
                List<Predicate> predicateList = new ArrayList<>();
                if (productSearchDto.getCategory() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("category"), productSearchDto.getCategory()));
                }
                if (productSearchDto.getProductName() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("name"), "%" + productSearchDto.getProductName() + "%"));
                }
                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i=0; i<predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }

                /// 위의 검색 조건들을 하나(한줄)의 Predicate 객체로 만들어서 return
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        return productRepository.findAll(specification, pageable).map(ProductResDto::fromEntity);
    }

    @Transactional
    public ProductResDto getProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));
        return ProductResDto.fromEntity(product);
    }
}
