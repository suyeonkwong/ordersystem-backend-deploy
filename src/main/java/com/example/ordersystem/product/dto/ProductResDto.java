package com.example.ordersystem.product.dto;

import com.example.ordersystem.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResDto {
    private Long id;
    private String name;
    private String category;
    private int price;
    private int stockQuantity;

    public static ProductResDto fromEntity(Product product) {
        return ProductResDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .build();
    }
}
