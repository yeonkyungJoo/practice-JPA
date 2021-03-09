package com.example.jpashop.modules.delivery;

import com.example.jpashop.modules.order.Order;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@Getter @Setter
public class Delivery {

    @Id @GeneratedValue
    private Long id;

    @OneToOne
    private Order order;
    private Address address;
    private DeliverStatus status;
}
