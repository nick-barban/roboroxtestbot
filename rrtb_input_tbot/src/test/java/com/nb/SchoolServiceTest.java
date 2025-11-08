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

import static org.junit.jupiter.api.Assertions.*;

class SchoolServiceTest extends AbstractTest {

    private static final String SCHOOL_TABLE_NAME = "School";
    
    @Inject
    private SchoolServiceImpl schoolService;
    
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
            key.put("id", item.get("id"));
            
            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(SCHOOL_TABLE_NAME)
                    .key(key)
                    .build();
            
            dynamoDbClient.deleteItem(deleteRequest);
        });
    }
    
    @Test
    void testAddSchoolFromLines() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: Test School From Lines",
            "description: Test Description From Lines",
            "location: Test Location From Lines",
            "telegramGroup: @testlinesgroup"
        };
        
        // When
        String schoolId = schoolService.addSchool(lines);
        
        // Then
        assertNotNull(schoolId);
        
        // Verify the school was added correctly
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        assertTrue(result.isPresent());
        
        Map<String, String> school = result.get();
        assertEquals(schoolId, school.get("id"));
        assertEquals("Test School From Lines", school.get("schoolName"));
        assertEquals("Test Description From Lines", school.get("description"));
        assertEquals("Test Location From Lines", school.get("location"));
        assertEquals("@testlinesgroup", school.get("telegramGroup"));
    }
    
    @Test
    void testAddSchoolFromLinesWithExtraWhitespace() {
        // Given
        String[] lines = {
            "  #addschool  ",
            "  schoolName:    Test School With Spaces   ",
            "description:      Test Description With Spaces   ",
            "   location:    Test Location With Spaces   ",
            "   telegramGroup:   @spacegroup   "
        };
        
        // When
        String schoolId = schoolService.addSchool(lines);
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        assertTrue(result.isPresent());
        
        Map<String, String> school = result.get();
        assertEquals("Test School With Spaces", school.get("schoolName"));
        assertEquals("Test Description With Spaces", school.get("description"));
        assertEquals("Test Location With Spaces", school.get("location"));
        assertEquals("@spacegroup", school.get("telegramGroup"));
    }
    
    @Test
    void testAddSchoolFromLinesMissingName() {
        // Given
        String[] lines = {
            "#addschool",
            "description: Description Without Name"
        };
        
        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolService.addSchool(lines);
        });
        
        assertEquals("School name is required", exception.getMessage());
    }
    
    @Test
    void testAddSchoolFromLinesWithMissingDescription() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: School Without Description",
            "location: Test Location",
            "telegramGroup: @testgroup"
        };
        
        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolService.addSchool(lines);
        });
        
        assertEquals("School description is required", exception.getMessage());
    }
    
    @Test
    void testAddSchoolFromLinesWithMissingLocation() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: School Without Location",
            "description: Test Description",
            "telegramGroup: @testgroup"
        };
        
        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolService.addSchool(lines);
        });
        
        assertEquals("School location is required", exception.getMessage());
    }
    
    @Test
    void testAddSchoolFromLinesWithMissingTelegramGroup() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: School Without Telegram Group",
            "description: Test Description",
            "location: Test Location"
        };
        
        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolService.addSchool(lines);
        });
        
        assertEquals("School telegram group is required", exception.getMessage());
    }
    
    @Test
    void testAddSchoolFromLinesWithInvalidFormat() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: Test School",
            "This line has no colon",
            "description: Test Description",
            "location: Test Location",
            "telegramGroup: @testgroup"
        };
        
        // When
        String schoolId = schoolService.addSchool(lines);
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        assertTrue(result.isPresent());
        
        // The invalid line should be ignored
        Map<String, String> school = result.get();
        assertEquals("Test School", school.get("schoolName"));
        assertEquals("Test Description", school.get("description"));
    }
    
    @Test
    void testAddSchoolFromLinesWithEmptyValue() {
        // Given
        String[] lines = {
            "#addschool",
            "schoolName: Test School",
            "description: Test Description",
            "location: Test Location",
            "telegramGroup: @testgroup",
            "emptyField: "
        };
        
        // When
        String schoolId = schoolService.addSchool(lines);
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(schoolId);
        assertTrue(result.isPresent());
        
        // The empty value field should be ignored
        Map<String, String> school = result.get();
        assertEquals("Test School", school.get("schoolName"));
        assertFalse(school.containsKey("emptyField"));
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
    void testUpdateNonExistentSchool() {
        // Given
        String nonExistentSchoolId = UUID.randomUUID().toString();
        Map<String, String> updates = new HashMap<>();
        updates.put("schoolName", "New School");
        updates.put("description", "New Description");
        
        // When
        schoolService.updateSchool(nonExistentSchoolId, updates);
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(nonExistentSchoolId);
        assertTrue(result.isPresent());
        assertEquals("New School", result.get().get("schoolName"));
        assertEquals("New Description", result.get().get("description"));
    }
    
    @Test
    void testDeleteNonExistentSchool() {
        // Given
        String nonExistentSchoolId = UUID.randomUUID().toString();
        
        // When - Should not throw exception
        assertDoesNotThrow(() -> {
            schoolService.deleteSchool(nonExistentSchoolId);
        });
        
        // Then
        Optional<Map<String, String>> result = schoolService.getSchool(nonExistentSchoolId);
        assertFalse(result.isPresent());
    }
} 