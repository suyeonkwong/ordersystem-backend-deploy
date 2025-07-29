package com.example.ordersystem.product.controller;

import com.example.ordersystem.common.dto.CommonDto;
import com.example.ordersystem.product.dto.ProductCreateDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@ModelAttribute ProductCreateDto productCreateDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonDto.builder()
                        .result(productService.saveProduct(productCreateDto))
                        .statusCode(HttpStatus.CREATED.value())
                        .statusMessage("상품 등록이 완료 되었습니다.").build());
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(Pageable pageable, ProductSearchDto productSearchDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonDto.builder()
                        .result(productService.getProductList(pageable, productSearchDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("상품 목록 조회가 완료 되었습니다.").build());
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> list(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonDto.builder()
                        .result(productService.getProduct(id))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("상품 조회가 완료 되었습니다.").build());
    }

}
