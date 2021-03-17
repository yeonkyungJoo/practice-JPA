package com.example.jpashop.modules.order;

import lombok.Data;

@Data
public class OrderSearch {

    private String memberName;
    private OrderStatus orderStatus;
}
