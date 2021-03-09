package com.example.jpashop.modules.orderItem;

import com.example.jpashop.modules.item.Item;
import com.example.jpashop.modules.order.Order;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "order_item")
@Getter @Setter
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;  // OrderItem - Item : 다대일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;    // OrderItem - Order : 다대일

    private int orderPrice;

    private int count;

}
