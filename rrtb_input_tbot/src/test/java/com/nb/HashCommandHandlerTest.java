package com.nb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.nb.service.SchoolService;

import io.micronaut.chatbots.core.Dispatcher;
import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.Send;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;

class HashCommandHandlerTest extends AbstractTest {

    @Inject
    private HashCommandHandler hashCommandHandler;

    @Inject
    private SchoolService schoolService;

    @Inject
    private Dispatcher<TelegramBotConfiguration, Update, Send> dispatcher;

    @Inject
    private JsonMapper jsonMapper;
    
    @MockBean(SpaceParser.class)
    SpaceParser<Update, Chat> spaceParser() {
        return Mockito.mock(SpaceParser.class);
    }
    
    @Inject
    private SpaceParser<Update, Chat> spaceParser;
    
    private Chat mockChat;

    @MockBean(SchoolService.class)
    SchoolService schoolService() {
        return Mockito.mock(SchoolService.class);
    }
    
    @BeforeEach
    void setUp() {
        // Initialize mockChat for use in tests
        mockChat = new Chat();
        mockChat.setId(123456L);
    }

    @Test
    void canHandleValidHashCommand() throws IOException {
        Update update = jsonMapper.readValue(getHashCommandJson(), Update.class);
        assertTrue(hashCommandHandler.canHandle(null, update));
    }

    @Test
    void cannotHandleNullMessage() {
        Update update = new Update();
        assertFalse(hashCommandHandler.canHandle(null, update));
    }

    @Test
    void cannotHandleNullText() {
        Update update = new Update();
        io.micronaut.chatbots.telegram.api.Message message = new io.micronaut.chatbots.telegram.api.Message();
        update.setMessage(message);
        assertFalse(hashCommandHandler.canHandle(null, update));
    }

    @Test
    void cannotHandleNonHashMessage() {
        Update update = new Update();
        io.micronaut.chatbots.telegram.api.Message message = new io.micronaut.chatbots.telegram.api.Message();
        message.setText("Hello world");
        update.setMessage(message);
        assertFalse(hashCommandHandler.canHandle(null, update));
    }

    @Test
    void handlesAddSchoolCommand() throws IOException {
        // Arrange
        Update update = jsonMapper.readValue(getHashCommandJson(), Update.class);
        Mockito.when(schoolService.addSchool(any(String[].class))).thenReturn("test-school-id");
        Mockito.when(spaceParser.parse(update)).thenReturn(Optional.of(mockChat));

        // Act
        Optional<SendMessage> result = hashCommandHandler.handle(null, update);

        // Assert
        assertTrue(result.isPresent());
        SendMessage message = result.get();
        assertEquals("School added successfully with ID: test-school-id", message.getText());
        
        // Verify service was called with correct parameters
        ArgumentCaptor<String[]> linesCaptor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(schoolService).addSchool(linesCaptor.capture());
        String[] capturedLines = linesCaptor.getValue();
        assertEquals(5, capturedLines.length); // Command + 4 parameters
        assertEquals("#addschool", capturedLines[0]);
        assertEquals("schoolName: Test School", capturedLines[1]);
    }

    @Test
    void handlesUnknownHashCommand() throws IOException {
        // Arrange
        Update update = jsonMapper.readValue(getInvalidHashCommandJson(), Update.class);
        Mockito.when(spaceParser.parse(update)).thenReturn(Optional.of(mockChat));

        // Act
        Optional<SendMessage> result = hashCommandHandler.handle(null, update);

        // Assert
        assertTrue(result.isPresent());
        SendMessage message = result.get();
        assertTrue(message.getText().contains("Unknown hash command"));
        
        // Verify school service was not called
        Mockito.verify(schoolService, never()).addSchool(any());
    }

    @Test
    void handlesInvalidAddSchoolCommand() throws IOException {
        // Arrange
        Update update = jsonMapper.readValue(getInvalidAddSchoolCommandJson(), Update.class);
        Mockito.when(spaceParser.parse(update)).thenReturn(Optional.of(mockChat));
        Mockito.when(schoolService.addSchool(any(String[].class)))
               .thenThrow(new IllegalArgumentException("School description is required"));

        // Act
        Optional<SendMessage> result = hashCommandHandler.handle(null, update);

        // Assert
        assertTrue(result.isPresent());
        SendMessage message = result.get();
        assertEquals("School description is required", message.getText());
    }

    @Test
    void dispatchesToHashCommandHandler() throws IOException {
        // Arrange
        Update update = jsonMapper.readValue(getHashCommandJson(), Update.class);
        Mockito.when(spaceParser.parse(update)).thenReturn(Optional.of(mockChat));
        Mockito.when(schoolService.addSchool(any(String[].class))).thenReturn("test-school-id");

        // Act
        Optional<Send> result = dispatcher.dispatch(null, update);

        // Assert
        assertTrue(result.isPresent());
        assertInstanceOf(SendMessage.class, result.get());
        SendMessage message = (SendMessage) result.get();
        assertEquals("School added successfully with ID: test-school-id", message.getText());
    }

    protected String getHashCommandJson() throws IOException {
        return new String(HashCommandHandlerTest.class.getResourceAsStream("/mockHashCommand.json").readAllBytes());
    }

    protected String getInvalidHashCommandJson() throws IOException {
        return new String(HashCommandHandlerTest.class.getResourceAsStream("/mockInvalidHashCommand.json").readAllBytes());
    }

    protected String getInvalidAddSchoolCommandJson() throws IOException {
        return new String(HashCommandHandlerTest.class.getResourceAsStream("/mockInvalidAddSchoolCommand.json").readAllBytes());
    }
} 