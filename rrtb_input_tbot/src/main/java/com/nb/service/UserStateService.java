package com.nb.service;

import com.nb.domain.RrtbCommand;
import com.nb.domain.common.RrtbUserState;
import io.micronaut.chatbots.telegram.api.Update;
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
public class UserStateService {
    public static final String USER_ID = "userId";
    public static final String COMMAND_TYPE = "commandType";
    public static final String STATE = "state";
    public static final String TTL = "ttl";
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "UserStateTable";
    private static final long STATE_EXPIRY_HOURS = 24;

    public UserStateService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void setPostState(String userId) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(USER_ID, AttributeValue.builder().s(userId).build());
        item.put(COMMAND_TYPE, AttributeValue.builder().s(RrtbCommand.POST.name()).build());
        item.put(STATE, AttributeValue.builder().s(RrtbUserState.WAITING_FOR_FILE.name()).build());
        item.put(TTL, AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(STATE_EXPIRY_HOURS * 3600).getEpochSecond())).build());

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public Optional<String> getPostState(String userId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_ID, AttributeValue.builder().s(userId).build());
        key.put(COMMAND_TYPE, AttributeValue.builder().s(RrtbCommand.POST.name()).build());

        final GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        return response.hasItem() ?
                Optional.of(response.item().get(STATE).s()) :
                Optional.empty();
    }

    public Map<String, String> getPostStateData(String userId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_ID, AttributeValue.builder().s(userId).build());
        key.put(COMMAND_TYPE, AttributeValue.builder().s(RrtbCommand.POST.name()).build());

        final GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        final Map<String, String> result = new HashMap<>();
        
        if (response.hasItem()) {
            final Map<String, AttributeValue> item = response.item();
            item.forEach((attrKey, value) -> {
                if (!attrKey.equals(USER_ID) && !attrKey.equals(COMMAND_TYPE) && !attrKey.equals(STATE) && !attrKey.equals(TTL)) {
                    result.put(attrKey, value.s());
                }
            });
        }
        
        return result;
    }

    public void updatePostState(Update update, String state, Map<String, String> data) {
        final String userId = String.valueOf(update.getMessage().getFrom().getId());
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(USER_ID, AttributeValue.builder().s(userId).build());
        item.put(COMMAND_TYPE, AttributeValue.builder().s("POST").build());
        item.put(STATE, AttributeValue.builder().s(state).build());
        item.put(TTL, AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(STATE_EXPIRY_HOURS * 3600).getEpochSecond())).build());

        data.forEach((key, value) -> 
            item.put(key, AttributeValue.builder().s(value).build())
        );

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public void clearPostState(Update update) {
        final String userId = String.valueOf(update.getMessage().getFrom().getId());
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_ID, AttributeValue.builder().s(userId).build());
        key.put(COMMAND_TYPE, AttributeValue.builder().s("POST").build());

        final DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
    }
} 