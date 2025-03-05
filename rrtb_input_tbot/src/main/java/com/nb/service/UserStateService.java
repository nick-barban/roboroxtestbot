package com.nb.service;

import io.micronaut.chatbots.telegram.api.Update;
import jakarta.inject.Singleton;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class UserStateService {
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "UserStateTable";
    private static final long STATE_EXPIRY_HOURS = 24;

    public UserStateService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void setPostState(Update update) {
        String userId = String.valueOf(update.getMessage().getFrom().getId());
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("commandType", AttributeValue.builder().s("POST").build());
        item.put("state", AttributeValue.builder().s("WAITING_FOR_FILE").build());
        item.put("ttl", AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(STATE_EXPIRY_HOURS * 3600).getEpochSecond())).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public Optional<String> getPostState(Update update) {
        String userId = String.valueOf(update.getMessage().getFrom().getId());
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        key.put("commandType", AttributeValue.builder().s("POST").build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem() ? 
                Optional.of(response.item().get("state").s()) : 
                Optional.empty();
    }

    public Map<String, String> getPostStateData(Update update) {
        String userId = String.valueOf(update.getMessage().getFrom().getId());
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        key.put("commandType", AttributeValue.builder().s("POST").build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        Map<String, String> result = new HashMap<>();
        
        if (response.hasItem()) {
            Map<String, AttributeValue> item = response.item();
            item.forEach((attrKey, value) -> {
                if (!attrKey.equals("userId") && !attrKey.equals("commandType") && !attrKey.equals("state") && !attrKey.equals("ttl")) {
                    result.put(attrKey, value.s());
                }
            });
        }
        
        return result;
    }

    public void updatePostState(Update update, String state, Map<String, String> data) {
        String userId = String.valueOf(update.getMessage().getFrom().getId());
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("commandType", AttributeValue.builder().s("POST").build());
        item.put("state", AttributeValue.builder().s(state).build());
        item.put("ttl", AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(STATE_EXPIRY_HOURS * 3600).getEpochSecond())).build());

        data.forEach((key, value) -> 
            item.put(key, AttributeValue.builder().s(value).build())
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public void clearPostState(Update update) {
        String userId = String.valueOf(update.getMessage().getFrom().getId());
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", AttributeValue.builder().s(userId).build());
        key.put("commandType", AttributeValue.builder().s("POST").build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
    }
} 