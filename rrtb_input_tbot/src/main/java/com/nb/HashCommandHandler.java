package com.nb;

import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.SendMessageUtils;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.chatbots.telegram.core.TelegramHandler;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for hash commands in Telegram messages.
 * Provides functionality for commands starting with # character.
 */
@Singleton
class HashCommandHandler implements TelegramHandler<SendMessage> {
    private static final Logger log = LoggerFactory.getLogger(HashCommandHandler.class);
    
    private final SpaceParser<Update, Chat> spaceParser;
    private final SchoolService schoolService;
    
    HashCommandHandler(SpaceParser<Update, Chat> spaceParser, SchoolService schoolService) {
        this.spaceParser = spaceParser;
        this.schoolService = schoolService;
    }
    
    @Override
    public boolean canHandle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        if (input.getMessage() == null || input.getMessage().getText() == null) {
            return false;
        }
        return input.getMessage().getText().startsWith("#");
    }
    
    @Override
    public Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        @Nullable
        final String text = input.getMessage().getText();
        
        try {
            String[] lines = text.split("\n");
            if (lines.length == 0) {
                return sendErrorResponse(input, "Empty message");
            }
            
            String firstLine = lines[0].trim();
            RrtbHashCommand command = parseCommand(firstLine);
            
            if (command == null) {
                return sendErrorResponse(input, "Unknown hash command: " + firstLine);
            }
            
            // Process command
            switch (command) {
                case ADD_SCHOOL:
                    return handleAddSchool(input, lines);
                default:
                    return sendErrorResponse(input, "Command not implemented: " + command.command());
            }
        } catch (Exception e) {
            log.error("Error processing hash command", e);
            return sendErrorResponse(input, "Error processing command: " + e.getMessage());
        }
    }
    
    /**
     * Handles the #addschool command
     * Format:
     * #addschool
     * schoolName: School Name
     * description: Description (optional)
     * location: Location (optional)
     * telegramGroup: @group (optional)
     */
    private Optional<SendMessage> handleAddSchool(Update input, String[] lines) {
        log.info("Processing add school command");
        
        try {
            // Let the SchoolService handle the parameter extraction and validation
            String schoolId = schoolService.addSchool(lines);
            
            return SendMessageUtils.compose(
                spaceParser,
                input,
                "School added successfully with ID: " + schoolId
            );
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return sendErrorResponse(input, e.getMessage());
        }
    }
    
    /**
     * Parses a command from the first line of a message
     */
    private RrtbHashCommand parseCommand(String commandText) {
        for (RrtbHashCommand cmd : RrtbHashCommand.values()) {
            if (commandText.startsWith(cmd.command())) {
                return cmd;
            }
        }
        return null;
    }
    
    /**
     * Creates an error response message
     */
    private Optional<SendMessage> sendErrorResponse(Update input, String errorMessage) {
        return SendMessageUtils.compose(
            spaceParser,
            input,
            errorMessage
        );
    }

    @Override
    public int getOrder() {
        return 3;
    }
} 