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
import java.util.UUID;

import com.nb.service.SchoolService;

/**
 * Service for managing school records in DynamoDB.
 */
@Singleton
public class SchoolServiceImpl implements SchoolService {
    // Table attribute constants
    public static final String SCHOOL_ID = "schoolId";
    public static final String SCHOOL_NAME = "schoolName";
    public static final String DESCRIPTION = "description";
    public static final String LOCATION = "location";
    public static final String TELEGRAM_GROUP = "telegramGroup";
    public static final String CREATED_AT = "createdAt";
    public static final String TTL = "ttl";
    
    private static final String TABLE_NAME = "School";
    private static final long SCHOOL_EXPIRY_DAYS = 365; // 1 year expiry
    
    private final DynamoDbClient dynamoDbClient;
    
    public SchoolServiceImpl(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }
    
    /**
     * Adds a new school by parsing command text lines.
     * 
     * @param lines Array of lines from the command message
     * @return The generated school ID
     * @throws IllegalArgumentException if required parameters are missing
     */
    @Override
    public String addSchool(String[] lines) {
        Map<String, String> params = parseParameters(lines);
        String schoolName = params.get(SCHOOL_NAME);
        
        if (schoolName == null || schoolName.isEmpty()) {
            throw new IllegalArgumentException("School name is required");
        }
        
        String schoolId = UUID.randomUUID().toString();
        String description = params.get(DESCRIPTION);
        String location = params.get(LOCATION);
        String telegramGroup = params.get(TELEGRAM_GROUP);
        
        Map<String, String> schoolData = new HashMap<>();
        schoolData.put("schoolId", schoolId);
        schoolData.put(SCHOOL_NAME, schoolName);
        
        if (description != null && !description.isEmpty()) {
            schoolData.put(DESCRIPTION, description);
        } else {
            throw new IllegalArgumentException("School description is required");
        }
        
        if (location != null && !location.isEmpty()) {
            schoolData.put(LOCATION, location);
        } else {
            throw new IllegalArgumentException("School location is required");
        }
        
        if (telegramGroup != null && !telegramGroup.isEmpty()) {
            schoolData.put(TELEGRAM_GROUP, telegramGroup);
        } else {
            throw new IllegalArgumentException("School telegram group is required");
        }
        
        saveSchool(schoolData);
        
        return schoolId;
    }
    
    /**
     * Parses parameters from message lines in key:value format
     */
    private Map<String, String> parseParameters(String[] lines) {
        Map<String, String> params = new HashMap<>();
        
        // Start from index 1 to skip the command line
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            int colonIndex = line.indexOf(':');
            
            if (colonIndex > 0 && colonIndex < line.length() - 1) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                
                if (!value.isEmpty()) {
                    params.put(key, value);
                }
            }
        }
        
        return params;
    }
    
    /**
     * Retrieves a school by its ID.
     * 
     * @param schoolId The unique identifier of the school
     * @return Optional containing school attributes if found, empty otherwise
     */
    @Override
    public Optional<Map<String, String>> getSchool(String schoolId) {
        final Map<String, AttributeValue> key = createSchoolIdKey(schoolId);

        final GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        
        if (!response.hasItem()) {
            return Optional.empty();
        }
        
        return Optional.of(convertDynamoItemToStringMap(response.item()));
    }
    
    /**
     * Updates a school's attributes while preserving existing values.
     * 
     * @param schoolId The unique identifier of the school
     * @param data Map of attribute names to new values
     */
    @Override
    public void updateSchool(String schoolId, Map<String, String> data) {
        // First retrieve the existing item
        final Map<String, AttributeValue> key = createSchoolIdKey(schoolId);

        final GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        final GetItemResponse getResponse = dynamoDbClient.getItem(getRequest);
        
        // Start with existing item or create a new one with schoolId if it doesn't exist
        final Map<String, AttributeValue> updatedItem;
        if (getResponse.hasItem()) {
            updatedItem = new HashMap<>(getResponse.item());
        } else {
            updatedItem = new HashMap<>();
            updatedItem.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());
        }
        
        // Apply updates
        data.forEach((updateKey, updateValue) -> 
            updatedItem.put(updateKey, AttributeValue.builder().s(updateValue).build())
        );
        
        // Update TTL to extend it
        updatedItem.put(TTL, createTtlAttribute());

        final PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(updatedItem)
                .build();

        dynamoDbClient.putItem(putRequest);
    }
    
    /**
     * Deletes a school from the database.
     * 
     * @param schoolId The unique identifier of the school to delete
     */
    @Override
    public void deleteSchool(String schoolId) {
        final Map<String, AttributeValue> key = createSchoolIdKey(schoolId);

        final DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
    }
    
    /**
     * Saves a school to the database using a map of attributes.
     * 
     * @param schoolData Map containing the school attributes
     */
    private void saveSchool(Map<String, String> schoolData) {
        final Map<String, AttributeValue> item = new HashMap<>();
        
        // Convert all string values to DynamoDB AttributeValues
        schoolData.forEach((key, value) -> 
            item.put(key, AttributeValue.builder().s(value).build())
        );
        
        // Add timestamp and TTL
        item.put(CREATED_AT, AttributeValue.builder().s(Instant.now().toString()).build());
        item.put(TTL, createTtlAttribute());

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
    
    // Helper methods
    
    private Map<String, AttributeValue> createSchoolIdKey(String schoolId) {
        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(SCHOOL_ID, AttributeValue.builder().s(schoolId).build());
        return key;
    }
    
    private void addOptionalAttribute(Map<String, AttributeValue> item, String attributeName, String value) {
        if (value != null && !value.isEmpty()) {
            item.put(attributeName, AttributeValue.builder().s(value).build());
        }
    }
    
    private AttributeValue createTtlAttribute() {
        long expiryEpochSeconds = Instant.now().plusSeconds(SCHOOL_EXPIRY_DAYS * 86400).getEpochSecond();
        return AttributeValue.builder().n(String.valueOf(expiryEpochSeconds)).build();
    }
    
    private Map<String, String> convertDynamoItemToStringMap(Map<String, AttributeValue> item) {
        final Map<String, String> result = new HashMap<>();
        
        item.forEach((attrKey, value) -> {
            if (value.s() != null) {
                result.put(attrKey, value.s());
            } else if (value.n() != null) {
                result.put(attrKey, value.n());
            }
        });
        
        return result;
    }
}
