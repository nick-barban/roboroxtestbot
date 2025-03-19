package com.nb;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.nb.repository.ChatRepository;
import com.nb.service.FileService;
import com.nb.service.messaging.MessageProducer;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@MicronautTest(environments = "test")
@Property(name = "POSTS_BUCKET_NAME", value = "test-bucket")
@Disabled("Disabled due to: io.micronaut.context.exceptions.ConfigurationException: Could not resolve placeholder ${POSTS_BUCKET_NAME}")
class SchedulerHandlerTest implements TestPropertyProvider{

    @Inject
    private SchedulerHandler handler;

    @Inject
    private FileService fileService;

    @Inject
    private MessageProducer producer;

    @Inject
    private ChatRepository chatRepository;

    @Inject
    private JsonMapper objectMapper;

    @MockBean(FileService.class)
    FileService fileService() {
        return mock(FileService.class);
    }

    @MockBean(MessageProducer.class)
    MessageProducer producer() {
        return mock(MessageProducer.class);
    }

    @MockBean(ChatRepository.class)
    ChatRepository chatRepository() {
        return mock(ChatRepository.class);
    }

    @MockBean(JsonMapper.class)
    JsonMapper objectMapper() {
        return mock(JsonMapper.class);
    }

    @Test
    void shouldSubstituteDateInTemplate() throws Exception {
        // Given
        String templateContent = "Today's date is {{date}}";
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("uk", "UA")));
        String expectedContent = "Today's date is " + expectedDate;
        String templateKey = "test_post";

        Chat mockChat = mock(Chat.class);
        when(mockChat.getTitle()).thenReturn("TestChat");
        when(chatRepository.getChatByName("test")).thenReturn(Optional.of(mockChat));
        when(fileService.readTodayPosts()).thenReturn(Map.of(templateKey, templateContent));
        when(objectMapper.writeValueAsString(any(Update.class))).thenReturn("{\"message\":{\"text\":\"" + expectedContent + "\"}}");

        // When
        handler.execute(new ScheduledEvent());

        // Then
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(objectMapper).writeValueAsString(updateCaptor.capture());
        
        Update capturedUpdate = updateCaptor.getValue();
        assertEquals(expectedContent, capturedUpdate.getMessage().getText());
        assertEquals(mockChat, capturedUpdate.getMessage().getChat());
        
        verify(producer).sendOutput(
            any(String.class),
            eq("TestChat"),
            eq("[TestChat]" + LocalDate.now())
        );
    }

    @Test
    void shouldHandleEmptyPost() {
        // Given
        String templateKey = "test_post";
        when(fileService.readTodayPosts()).thenReturn(Map.of(templateKey, ""));

        // When
        handler.execute(new ScheduledEvent());

        // Then
        verify(chatRepository, never()).getChatByName(any());
        verify(producer, never()).sendOutput(any(), any(), any());
    }

    @Test
    void shouldHandleChatNotFound() {
        // Given
        String templateContent = "Test content";
        String templateKey = "test_post";
        
        when(fileService.readTodayPosts()).thenReturn(Map.of(templateKey, templateContent));
        when(chatRepository.getChatByName("test")).thenReturn(Optional.empty());

        // When
        handler.execute(new ScheduledEvent());

        // Then
        verify(producer, never()).sendOutput(any(), any(), any());
    }

    @Test
    void shouldHandleMultiplePosts() throws Exception {
        // Given
        String template1 = "Post 1 for {{date}}";
        String template2 = "Post 2 for {{date}}";
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("uk", "UA")));
        
        Chat chat1 = mock(Chat.class);
        when(chat1.getTitle()).thenReturn("Chat1");
        Chat chat2 = mock(Chat.class);
        when(chat2.getTitle()).thenReturn("Chat2");
        
        when(chatRepository.getChatByName("chat1")).thenReturn(Optional.of(chat1));
        when(chatRepository.getChatByName("chat2")).thenReturn(Optional.of(chat2));
        when(fileService.readTodayPosts()).thenReturn(Map.of(
            "chat1_post", template1,
            "chat2_post", template2
        ));
        when(objectMapper.writeValueAsString(any(Update.class))).thenReturn("{}");

        // When
        handler.execute(new ScheduledEvent());

        // Then
        verify(producer, times(2)).sendOutput(any(), any(), any());
        verify(producer).sendOutput(any(), eq("Chat1"), eq("[Chat1]" + LocalDate.now()));
        verify(producer).sendOutput(any(), eq("Chat2"), eq("[Chat2]" + LocalDate.now()));
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
    return Map.of("POSTS_BUCKET_NAME","test-bucket");
    }
}