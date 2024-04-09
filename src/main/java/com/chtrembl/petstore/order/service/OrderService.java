package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.model.Order;

public interface OrderService {

    void save(Order order);

    Order findById(String id);
}
