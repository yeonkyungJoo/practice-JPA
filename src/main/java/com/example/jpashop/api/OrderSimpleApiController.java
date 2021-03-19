package com.example.jpashop.api;

import com.example.jpashop.modules.member.Address;
import com.example.jpashop.modules.order.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    // 주문 + 배송정보 + 회원을 조회하는 API
    // 지연 로딩 때문에 발생하는 성능 문제 해결

    // XToOne(ManyToOne, OneToOne) 관계 최적화
    // Order
    // Order -> Member
    // Order -> Delivery

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * v1. 엔티티 직접 노출
     * - Hibernate5Module 등록, LAZY = null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for(Order order : all) {
            // LAZY 강제 초기화
            order.getMember().getName();
            order.getDelivery().getAddress();
        }
        return all;

        // order - member와 order - address는 지연 로딩
        // 따라서 실제 엔티티 대신에 프록시가 존재한다.
        // jackson 라이브러리는 기본적으로 이 프록시 객체를 json으로 어떻게 생성해야 하는지 몰라 예외가 발생한다.
        // Hibernate5Module을 스프링 빈으로 등록하면 해결된다.
    }

    /**
     * v2. 엔티티를 조회해서 DTO로 변환,  fetch join 사용 X
     * - 단점 : 지연로딩으로 쿼리 N번 호출
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<SimpleOrderDto> collect = orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
        return collect;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }


    /**
     * v3. 엔티티를 조회해서 DTO로 변환,  fetch join 사용 O
     * - fetch join으로 쿼리 1번 호출
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        // 페치 조인으로 order -> member, order -> delivery는 이미 조회된 상태이므로 지연로딩 X
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> collect = orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * v4. JPA에서 DTO로 바로 조회
     * - 쿼리 1번 호출
     * - select 절에서 원하는 데이터만 선택해서 조회
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

}
