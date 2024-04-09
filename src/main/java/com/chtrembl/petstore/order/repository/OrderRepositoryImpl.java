package com.chtrembl.petstore.order.repository;

import javax.annotation.PostConstruct;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.chtrembl.petstore.order.model.Order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private CosmosContainer container;

    @Value("${cosmos.key}")
    private String cosmosKey;

    @Value("${cosmos.uri}")
    private String cosmosUri;

    @PostConstruct
    public void setUp() {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(cosmosUri)
                .key(cosmosKey)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        CosmosDatabase database = client.getDatabase("orders");
        container = database.getContainer("orders_container");
    }

    @Override
    public void save(final Order order) {
        String id = order.getId();

        Order existingOrder = findById(order.getId());

        if (existingOrder == null) {
           container.createItem(order,
                    new PartitionKey(id),
                    new CosmosItemRequestOptions());
        } else {
            container.replaceItem(order,
                    id,
                    new PartitionKey(id),
                    new CosmosItemRequestOptions());
        }
    }

    @Override
    public Order findById(final String id) {
        try {
            return container.readItem(id, new PartitionKey(id), Order.class).getItem();
        } catch (NotFoundException e) {
            return null;
        }
    }
}
