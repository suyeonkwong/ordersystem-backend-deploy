package com.example.ordersystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonErrorDto {
    private int statusCode;
    private String statusMessage;
}
