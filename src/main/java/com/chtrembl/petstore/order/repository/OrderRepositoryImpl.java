package com.chtrembl.petstore.order.repository;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.chtrembl.petstore.order.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);

    private static final String DATABASE_NAME = "orders";
    private static final String CONTAINER_NAME = "order_container";

    private CosmosContainer container;

    @Value("${cosmos.key}")
    private String cosmosKey;

    @Value("${cosmos.uri}")
    private String cosmosUri;

    /**
     * Initializes the Cosmos DB client and sets up the container connection.
     * Called automatically after dependency injection is complete.
     */
    @PostConstruct
    public void setUp() {
        try {
            logger.info("Initializing Cosmos DB connection to URI: {}", cosmosUri);

            CosmosClient client = new CosmosClientBuilder()
                    .endpoint(cosmosUri)
                    .key(cosmosKey)
                    .consistencyLevel(ConsistencyLevel.STRONG)
                    .contentResponseOnWriteEnabled(true)
                    .buildClient();

            CosmosDatabase database = client.getDatabase(DATABASE_NAME);
            container = database.getContainer(CONTAINER_NAME);

            logger.info("Successfully connected to Cosmos DB database: {} container: {}",
                    DATABASE_NAME, CONTAINER_NAME);
        } catch (Exception e) {
            logger.error("Failed to initialize Cosmos DB connection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Cosmos DB connection", e);
        }
    }

    /**
     * Saves an order to the Cosmos DB container.
     * Creates a new item if it doesn't exist, otherwise replaces the existing one.
     *
     * @param order the order to save
     * @throws IllegalArgumentException if the order or its ID is null
     */
    @Override
    public void save(final Order order) {
        validateOrder(order);

        String id = order.getId();
        logger.debug("Saving order with ID: {}", id);

        try {
            if (findById(id).isPresent()) {
                logger.debug("Replacing existing order with ID: {}", id);
                container.replaceItem(
                        order,
                        id,
                        new PartitionKey(id),
                        new CosmosItemRequestOptions()
                );
            } else {
                logger.debug("Creating new order with ID: {}", id);
                container.createItem(
                        order,
                        new PartitionKey(id),
                        new CosmosItemRequestOptions()
                );
            }
            logger.info("Successfully saved order with ID: {}", id);
        } catch (Exception e) {
            logger.error("Failed to save order with ID: {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to save order with ID: " + id, e);
        }
    }

    /**
     * Finds an order by its ID.
     *
     * @param id the ID of the order to find
     * @return an Optional containing the order if found, or empty if not found
     * @throws IllegalArgumentException if the ID is null or empty
     */
    @Override
    public Optional<Order> findById(final String id) {
        validateId(id);

        logger.debug("Looking up order with ID: {}", id);

        try {
            Order order = container
                    .readItem(id, new PartitionKey(id), Order.class)
                    .getItem();
            logger.debug("Found order with ID: {}", id);
            return Optional.ofNullable(order);
        } catch (NotFoundException e) {
            logger.debug("No order found with ID: {}", id);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error retrieving order with ID: {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve order with ID: " + id, e);
        }
    }

    /**
     * Validates that an order is not null and has a valid ID.
     *
     * @param order the order to validate
     * @throws IllegalArgumentException if the order or its ID is null
     */
    private void validateOrder(final Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        validateId(order.getId());
    }

    /**
     * Validates that an ID is not null or empty.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is null or empty
     */
    private void validateId(final String id) {
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
    }
}
