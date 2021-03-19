package com.example.jpashop.modules.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        // toOne 코드를 모두 한번에 조회
        List<OrderQueryDto> result = findOrders();

        // 루프를 돌면서 컬렉션 추가
        result.forEach(o -> {
            List<OrderItemQueyDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });
        return result;
    }

    private List<OrderQueryDto> findOrders() {

        return em.createQuery("select new com.example.jpashop.modules.order.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderQueryDto.class).getResultList();
    }

    // 1:N 관계인 orderItems 조회
    private List<OrderItemQueyDto> findOrderItems(Long orderId) {
        return em.createQuery("select new com.example.jpashop.modules.order.OrderItemQueyDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                    " from OrderItem oi" +
                                    " join oi.item i" +
                                    " where oi.order.id = :orderId", OrderItemQueyDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    public List<OrderQueryDto> findAllByDto_optimization() {

        // 루트 조회
        List<OrderQueryDto> result = findOrders();

        // orderItem 컬렉션을 MAP 한방에 조회
        Map<Long, List<OrderItemQueyDto>> orderItemMap = findOrderItemMap(toOrderIds(result));
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        return result;
    }

    private Map<Long, List<OrderItemQueyDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueyDto> orderItems = em.createQuery(
                "select new com.example.jpashop.modules.order.OrderItemQueyDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id in :orderIds", OrderItemQueyDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        return orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueyDto::getOrderId));

    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream().map(o -> o.getOrderId()).collect(Collectors.toList());
    }

    
}
