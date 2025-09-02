package com.example.ordersystem.ordering.service;

import com.example.ordersystem.common.service.SseAlarmService;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.OrderDetailDto;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final SseAlarmService sseAlarmService;

    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, SseAlarmService sseAlarmService) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.sseAlarmService = sseAlarmService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)    /// 격리레벨을 낮춤으로서, 성능향상과 lock 관련 문제 원천 차단
    public Long createConcurrent(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();
        Long id = orderingRepository.save(ordering).getId();

        for (OrderCreateDto orderCreateDto : orderCreateDtoList) {
            Product product = productRepository.findById(orderCreateDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

            /// redis 에서 재고수량 확인 및 재고수량 감소처리
//            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), orderCreateDto.getProductCount());
//            if(newQuantity < 0) {
//                throw new IllegalArgumentException("재고부족");
//            }

            /// 1. 동시에 접근하는 상황에서 update 값에 저합성이 깨지고 갱신이상이 발생
            /// 2. spring 버전이나 mysql 버전에 따라 jpa 에서 강제에러(deadlock)를 유발시켜 대부분의 요청실패 발생
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(orderCreateDto.getProductCount())
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetailList().add(orderDetail);     // cascade 사용
            /// rdb에 사후 update를 위한 메시지 발행(비동기 처리)
//            stockRabbitMqService.publish(orderCreateDto.getProductId(), orderCreateDto.getProductCount());
        }

        // 주문 성공 시 admin 유저에게 알림 메시지 전송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());
        return id;
    }

    public Long create(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();
        Long id = orderingRepository.save(ordering).getId();

        for (OrderCreateDto orderCreateDto : orderCreateDtoList) {
            Product product = productRepository.findById(orderCreateDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

            /// redis 에서 재고수량 확인 및 재고수량 감소처리
//            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), orderCreateDto.getProductCount());
//            if(newQuantity < 0) {
//                throw new IllegalArgumentException("재고부족");
//            }

            /// 1. 동시에 접근하는 상황에서 update 값에 저합성이 깨지고 갱신이상이 발생
            /// 2. spring 버전이나 mysql 버전에 따라 jpa 에서 강제에러(deadlock)를 유발시켜 대부분의 요청실패 발생
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(orderCreateDto.getProductCount())
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetailList().add(orderDetail);     // cascade 사용
        }
        // 주문 성공 시 admin 유저에게 알림 메시지 전송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());
        return id;
    }

    public List<OrderListResDto> orderingList() {
        List<Ordering> orderingList = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();

        for (Ordering ordering : orderingList) {
            List<OrderDetail> orderDetailList = ordering.getOrderDetailList();
            List<OrderDetailDto> orderDetailDtoList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetailDtoList.add(OrderDetailDto.fromEntity(orderDetail));
            }
            orderListResDtoList.add(OrderListResDto.builder()
                    .id(ordering.getId())
                    .memberEmail(ordering.getMember().getEmail())
                    .orderStatus(ordering.getOrderStatus())
                    .orderDetails(orderDetailDtoList)
                    .build());
        }

        return orderListResDtoList;
    }

    public List<OrderListResDto> myOrders() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("엔티티 없음"));
        List<Ordering> orderingList = orderingRepository.findAllByMember(member);
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();

        for (Ordering ordering : orderingList) {
            List<OrderDetail> orderDetailList = ordering.getOrderDetailList();
            List<OrderDetailDto> orderDetailDtoList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetailDtoList.add(OrderDetailDto.builder()
                        .detailId(orderDetail.getId())
                        .productName(orderDetail.getProduct().getName())
                        .productCount(orderDetail.getQuantity())
                        .build());
            }
            orderListResDtoList.add(OrderListResDto.builder()
                    .id(ordering.getId())
                    .memberEmail(ordering.getMember().getEmail())
                    .orderStatus(ordering.getOrderStatus())
                    .orderDetails(orderDetailDtoList)
                    .build());
        }

        return orderListResDtoList;
    }
}
