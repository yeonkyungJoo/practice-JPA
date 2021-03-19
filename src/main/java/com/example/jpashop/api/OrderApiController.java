package com.example.jpashop.api;

import com.example.jpashop.modules.member.Address;
import com.example.jpashop.modules.order.*;
import com.example.jpashop.modules.orderItem.OrderItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.sql.DataSourceDefinitions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * v1. 엔티티 직접 노출
     * - 엔티티가 변하면 API 스펙이 변한다
     * - 트랜잭션 안에서 지연로딩 필요
     * - 양방향 연관관계 문제
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for (Order order : all) {
            
            // LAZY 강제 초기화
            // orderItem, item 관계를 직접 초기화하면
            // Hibernate5Module 설정에 의해 엔티티를 JSON으로 생성한다.
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(orderItem -> orderItem.getItem().getName());
        }
        return all;
    }

    /**
     * v2. 엔티티를 조회해서 DTO로 변환, fetch join 사용 X
     * - 트랜잭션 안에서 지연로딩 필요
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> collect = orders.stream()
                .map(order -> new OrderDto(order))
                .collect(Collectors.toList());
        return collect;
    }

    // 지연로딩으로 너무 많은 SQL 실행
    // order - 1번
    // member, address - order 개수 N번
    // orderItem - order 개수 N번
    // item - orderItem 개수 M번

    // 지연로딩은 영속성 컨텍스트에 있으면 영속성 컨텍스트에 있는 엔티티를 사용하고 없으면 SQL을 실행한다.
    // 따라서 같은 영속성 컨텍스트에서 이미 로딩한 회원 엔티티를 추가로 조회하면 SQL을 실행하지 않는다.

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            this.itemName = orderItem.getItem().getName();
            this.orderPrice = orderItem.getOrderPrice();
            this.count = orderItem.getCount();
        }
    }


    /**
     * v3. 엔티티를 조회해서 DTO로 변환, fetch join 사용 O
     * - 페이징 시에는 N부분을 포기해야 한다.
     * - 대신에 batch fetch size 옵션 주면 N -> 1 쿼리로 변경 가능
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {

        /*
        select distinct * from orders
        join member on orders.member_id = member.member_id
        join delivery on orders.delivery_id = delivery.id
        join order_item on orders.order_id = order_item.order_id
        join item on order_item.item_id = item.item_id;
        */

        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(order -> new OrderDto(order)).collect(Collectors.toList());

        return collect;
    }

    // - 페치 조인으로 SQL이 1번만 실행된다.
    // - distinct를 사용한 이유
    // : 1대다 조인이 있으므로 데이터베이스 row가 증가한다. 그 결과 같은 order 엔티티의 조회 수도
    // 증가하게 된다. JPA의 distinct는 SQL에 distinct를 추가하고,
    // 더해서 같은 엔티티가 조회되면 애플리케이션에서 중복을 걸러준다.
    // 이 예에서는 order가 컬렉션 페치 조인 때문에 중복 조회되는 것을 막아준다.
    // - 컬렉션을 페치 조인하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다. (매우 위험)
    // : 컬렉션을 페치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.
    // 일대다에서 일(1)을 기준으로 페이징을 하는 것이 목적이다. 그런데 데이터는 다(N)를 기준으로 row가 생성된다.
    // - 컬렉션 페치 조인은 1개만 사용 가능

    /**
     * v3-1. 페이징 + 컬렉션 엔티티 조회
     * 1) 먼저 ToOne(OneToOne, ManyToOne) 관계를 모두 페치 조인한다.
     * ToOne 관계는 row수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
     * 2) 컬렉션은 지연로딩으로 조회한다.
     * 3) 지연로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize를 적용한다.
     * - hibernate.default_batch_fetch_size : 글로벌 설정
     * - @BatchSize : 개별 최적화
     * - 이 옵션을 사용하면 컬렉션이나 프록시 객체를 한꺼번에 설정한 size만큼 IN쿼리로 조회한다.
     */
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> collect = orders.stream()
                .map(order -> new OrderDto(order)).collect(Collectors.toList());

        return collect;
    }

    /**
     * v4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
     * - 페이징 가능
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * v5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
     * - 페이징 가능
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * v6. JPA에서 DTO로 바로 조회, 플랫 데이터 (1Query)
     * - 페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
    }
}
