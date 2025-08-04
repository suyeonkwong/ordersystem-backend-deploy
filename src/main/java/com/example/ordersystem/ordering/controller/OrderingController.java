package com.example.ordersystem.ordering.controller;

import com.example.ordersystem.common.dto.CommonDto;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.service.OrderingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordering")
public class OrderingController {
    private final OrderingService orderingService;

    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody List<OrderCreateDto> orderCreateDtoList) {
        Long id = orderingService.createConcurrent(orderCreateDtoList);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("주문 완료")
                        .build(),
                HttpStatus.OK);
    }

    /// 객체 안에 객체가 있을 경우
//    @PostMapping("/create")
//    public ResponseEntity<?> create(@RequestBody OrderDetailDto orderDetailDto){
//        System.out.println(orderDetailDto);
//        return null;
//    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> orderList(){
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderingService.orderingList())
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("주문 조회")
                        .build(),
                HttpStatus.OK);
    }

    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders(){
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderingService.myOrders())
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("내 주문 조회")
                        .build(),
                HttpStatus.OK);
    }


}
