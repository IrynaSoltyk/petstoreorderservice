package com.chtrembl.petstore.order.repository;

import com.chtrembl.petstore.order.model.Order;

public interface OrderRepository {

    void save(Order order);

    Order findById(String id);
}
