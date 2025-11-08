package com.nb.service;

import io.micronaut.chatbots.telegram.api.Update;
import jakarta.inject.Singleton;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class PostService {
    private final DynamoDbClient dynamoDbClient;
    private static final String GROUP_TABLE_NAME = "GroupTable";

    public PostService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void savePostData(Update update, String fileName, String chatId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(chatId).build());
        item.put("chatId", AttributeValue.builder().s(chatId).build());
        item.put("name", AttributeValue.builder().s(fileName).build());
        item.put("type", AttributeValue.builder().s("TELEGRAM").build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(GROUP_TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public Optional<String> getGroupName(String chatId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(chatId).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(GROUP_TABLE_NAME)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem() ? 
                Optional.of(response.item().get("name").s()) : 
                Optional.empty();
    }
} 