package com.nb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SchoolServiceTest extends AbstractTest {

    private static final String SCHOOL_TABLE_NAME = "School";
    
    @Inject
    private SchoolService schoolService;
    
    @Inject
    private DynamoDbClient dynamoDbClient;
    
    @BeforeEach
    void clearSchoolTable() {
        // Clear the table data before each test
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(SCHOOL_TABLE_NAME)
                .build();
        
        ScanResponse response = dynamoDbClient.scan(scanRequest);
        response.items().forEach(item -> {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("schoolId", item.get("schoolId"));
            
            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(SCHOOL_TABLE_NAME)
                    .key(key)
                    .build();
            
            dynamoDbClient.deleteItem(deleteRequest);
        });
    }
    
    @Test
    void testAddSchool() {
        // Given
        String schoolId = UUID.randomUUID().toString();
        String schoolName = "Test School";
        String description = "Test Description";
        String location = "Test Location";
        String telegramGroup = "@testgroup";
        
        // When
        schoolService.addSchool(schoolId, schoolName, description, location, telegramGroup);
        
        // Then
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("schoolId", AttributeValue.builder().s(schoolId).build());
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(SCHOOL_TABLE_NAME)
                .key(key)
                .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        assertTrue(response.hasItem());
        assertEquals(schoolName, response.item().get("schoolName").s());
        assertEquals(description, response.item().get("description").s());
        assertEquals(location, response.item().get("location").s());
        assertEquals(telegramGroup, response.item().get("telegramGroup").s());
    }
    
    @Test
    void testAddSchoolWithNullFields() {
        // Given
        String schoolId = UUID.randomUUID().toString();
        String schoolName = "Test School Only Name";
        
        // When
        schoolService.addSchool(schoolId, schoolName, null, null, null);
        
        // Then
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("schoolId", AttributeValue.builder().s(schoolId).build());
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(SCHOOL_TABLE_NAME)
                .key(key)
                .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        assertTrue(response.hasItem());
        assertEquals(schoolName, response.item().get("schoolName").s());
        assertFalse(response.item().containsKey("description"));
        assertFalse(response.item().containsKey("location"));
        assertFalse(response.item().containsKey("telegramGroup"));
    }
    
    @Test
    void testGetSchool() {
        // Given
        String schoolId = UUID.randomUUID().toString();
        String schoolName = "Test School for Get";
        String description = "Test Description for Get";
        String location = "Test Location for Get";
        String telegramGroup = "@getgroup";
        
        schoolService.addSchool(schoolId, schoolName, description, location, telegramGroup);
        
        // When
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        
        // Then
        assertTrue(result.isPresent());
        Map<String, String> school = result.get();
        assertEquals(schoolId, school.get("schoolId"));
        assertEquals(schoolName, school.get("schoolName"));
        assertEquals(description, school.get("description"));
        assertEquals(location, school.get("location"));
        assertEquals(telegramGroup, school.get("telegramGroup"));
    }
    
    @Test
    void testGetSchoolNotFound() {
        // Given
        String nonExistentSchoolId = "non-existent-id";
        
        // When
        Optional<Map<String, String>> result = schoolService.getSchool(nonExistentSchoolId);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void testUpdateSchool() {
        // Given
        String schoolId = UUID.randomUUID().toString();
        String initialSchoolName = "Initial School Name";
        String initialDescription = "Initial Description";
        
        schoolService.addSchool(schoolId, initialSchoolName, initialDescription, null, null);
        
        Map<String, String> updates = new HashMap<>();
        String updatedSchoolName = "Updated School Name";
        String updatedLocation = "Updated Location";
        String updatedTelegramGroup = "@updatedgroup";
        
        updates.put("schoolName", updatedSchoolName);
        updates.put("location", updatedLocation);
        updates.put("telegramGroup", updatedTelegramGroup);
        
        // When
        schoolService.updateSchool(schoolId, updates);
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        assertTrue(result.isPresent());
        Map<String, String> updatedSchool = result.get();
        
        assertEquals(updatedSchoolName, updatedSchool.get("schoolName"));
        // Description should still be present and unchanged
        assertEquals(initialDescription, updatedSchool.get("description"));
        assertEquals(updatedLocation, updatedSchool.get("location"));
        assertEquals(updatedTelegramGroup, updatedSchool.get("telegramGroup"));
    }
    
    @Test
    void testDeleteSchool() {
        // Given
        String schoolId = UUID.randomUUID().toString();
        String schoolName = "School to Delete";
        
        schoolService.addSchool(schoolId, schoolName, null, null, null);
        
        // Verify it exists first
        Optional<Map<String, String>> beforeDelete = schoolService.getSchool(schoolId);
        assertTrue(beforeDelete.isPresent());
        
        // When
        schoolService.deleteSchool(schoolId);
        
        // Then
        Optional<Map<String, String>> afterDelete = schoolService.getSchool(schoolId);
        assertFalse(afterDelete.isPresent());
        
        // Also verify directly with DynamoDB client
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("schoolId", AttributeValue.builder().s(schoolId).build());
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(SCHOOL_TABLE_NAME)
                .key(key)
                .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        assertFalse(response.hasItem());
    }
} 