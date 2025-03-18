package com.nb.service;

import java.util.Map;
import java.util.Optional;

public interface SchoolService {

    /**
     * Adds a new school by parsing command text lines.
     * 
     * @param lines Array of lines from the command message
     * @return The generated school ID
     * @throws IllegalArgumentException if required parameters are missing
     */
    String addSchool(String[] lines);

    /**
     * Retrieves a school by its ID.
     * 
     * @param schoolId The unique identifier of the school
     * @return Optional containing school attributes if found, empty otherwise
     */
    Optional<Map<String, String>> getSchool(String schoolId);

    /**
     * Updates a school's attributes while preserving existing values.
     * 
     * @param schoolId The unique identifier of the school
     * @param data Map of attribute names to new values
     */
    void updateSchool(String schoolId, Map<String, String> data);

    /**
     * Deletes a school from the database.
     * 
     * @param schoolId The unique identifier of the school to delete
     */
    void deleteSchool(String schoolId);

}