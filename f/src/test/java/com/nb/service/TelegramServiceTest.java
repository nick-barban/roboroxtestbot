package com.nb.service;

import com.nb.config.TelegramConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelegramServiceTest {

    @Mock
    private MessageSender mockMessageSender;
    @Mock
    private TelegramConfig mockConfig;

    @InjectMocks
    private TelegramService telegramService;

    @BeforeEach
    void setUp() throws TelegramApiException {
        MockitoAnnotations.openMocks(this);
        
        when(mockConfig.getBotUsername()).thenReturn("test-username");

        when(mockMessageSender.execute(any(SendMessage.class))).thenReturn(null); 
    }

    @Test
    void constructor_shouldInitializeCorrectly() {
        assertNotNull(telegramService);
    }

    @Test
    void sendMessage_shouldCallSenderExecuteWithCorrectData() throws TelegramApiException {
        Long chatId = 12345L;
        String text = "Hello World";
        String escapedText = text;

        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(chatId);
        when(mockMessage.getText()).thenReturn(text);

        telegramService.sendMessage(mockMessage);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(mockMessageSender).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();
        assertEquals(chatId.toString(), sentMessage.getChatId());
        assertEquals(escapedText, sentMessage.getText());
        assertEquals("html", sentMessage.getParseMode());
        
        verify(mockConfig).getBotUsername(); 
    }
    
    @Test
    void sendMessage_shouldEscapeHashTagsBeforeSending() throws TelegramApiException {
        Long chatId = 12345L;
        String textWithHash = "#Hello #World";
        String expectedEscapedText = "\\#Hello \\#World";

        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(chatId);
        when(mockMessage.getText()).thenReturn(textWithHash);

        telegramService.sendMessage(mockMessage);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(mockMessageSender).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();
        assertEquals(chatId.toString(), sentMessage.getChatId());
        assertEquals(expectedEscapedText, sentMessage.getText());
        assertEquals("html", sentMessage.getParseMode());
        
        verify(mockConfig).getBotUsername(); 
    }

    @Test
    void sendMessage_nullMessageText_shouldSendNull() throws TelegramApiException {
        Long chatId = 12345L;
        
        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(chatId);
        when(mockMessage.getText()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            telegramService.sendMessage(mockMessage);
        }, "Expected NullPointerException because SendMessage.setText does not accept null");
        
        verify(mockConfig).getBotUsername();
        verify(mockMessageSender, never()).execute(any(SendMessage.class));
    }


    @Test
    void sendMessage_shouldHandleTelegramApiException() throws TelegramApiException {
        Long chatId = 12345L;
        String text = "Test exception";
        TelegramApiException exception = new TelegramApiException("Execution failed");

        Message mockMessage = mock(Message.class);
        when(mockMessage.getChatId()).thenReturn(chatId);
        when(mockMessage.getText()).thenReturn(text);

        when(mockMessageSender.execute(any(SendMessage.class))).thenThrow(exception);

        assertDoesNotThrow(() -> telegramService.sendMessage(mockMessage));

        verify(mockMessageSender).execute(any(SendMessage.class));
        
        verify(mockConfig).getBotUsername(); 
    }
} 