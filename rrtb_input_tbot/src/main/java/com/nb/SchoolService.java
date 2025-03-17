package com.nb;

import jakarta.inject.Singleton;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class SchoolService {
    public static final String SCHOOL_ID = "schoolId";
    public static final String SCHOOL_NAME = "schoolName";
    public static final String DESCRIPTION = "description";
    public static final String LOCATION = "location";
    private static final String TELEGRAM_GROUP = "telegramGroup";
    public static final String CREATED_AT = "createdAt";
    public static final String TTL = "ttl";
    
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "School";
    private static final long SCHOOL_EXPIRY_DAYS = 365; // 1 year expiry
        
        public SchoolService(DynamoDbClient dynamoDbClient) {
            this.dynamoDbClient = dynamoDbClient;
        }
        
        public void addSchool(String schoolId, String schoolName, String description, String location, String telegramGroup) {
            final Map<String, AttributeValue> item = new HashMap<>();
            item.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());
            item.put(SCHOOL_NAME, AttributeValue.builder().s(schoolName).build());
            
            if (description != null && !description.isEmpty()) {
                item.put(DESCRIPTION, AttributeValue.builder().s(description).build());
            }
            
            if (location != null && !location.isEmpty()) {
                item.put(LOCATION, AttributeValue.builder().s(location).build());
            }
            
            if (telegramGroup != null && !telegramGroup.isEmpty()) {
                item.put(TELEGRAM_GROUP, AttributeValue.builder().s(telegramGroup).build());
        }

        item.put(CREATED_AT, AttributeValue.builder().s(Instant.now().toString()).build());
        item.put(TTL, AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(SCHOOL_EXPIRY_DAYS * 86400).getEpochSecond())).build());

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
    
    public Optional<Map<String, String>> getSchool(String schoolId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());

        final GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        
        if (!response.hasItem()) {
            return Optional.empty();
        }
        
        final Map<String, String> result = new HashMap<>();
        final Map<String, AttributeValue> item = response.item();
        
        item.forEach((attrKey, value) -> {
            if (value.s() != null) {
                result.put(attrKey, value.s());
            } else if (value.n() != null) {
                result.put(attrKey, value.n());
            }
        });
        
        return Optional.of(result);
    }
    
    public void updateSchool(String schoolId, Map<String, String> data) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());
        
        data.forEach((key, value) -> 
            item.put(key, AttributeValue.builder().s(value).build())
        );
        
        // Update TTL to extend it
        item.put(TTL, AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(SCHOOL_EXPIRY_DAYS * 86400).getEpochSecond())).build());

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
    
    public void deleteSchool(String schoolId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());

        final DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
    }
}
