package com.nb.service;

import com.nb.config.TelegramConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Singleton
public class TelegramService {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramService.class);
    private final MessageSender messageSender;
    private final TelegramConfig config;

    @Inject
    public TelegramService(MessageSender messageSender, TelegramConfig config) {
        this.messageSender = messageSender;
        this.config = config;
    }

    private String escapeHashTags(String message) {
        if (message == null) {
            return null;
        }
        return message.replace("#", "\\#");
    }

    private void sendMessageInternal(Long chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setParseMode(ParseMode.HTML);
            messageSender.execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Error sending message: {}", e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        LOG.debug("Sending message via injected sender. Bot username from config: {}", this.config.getBotUsername());
        final Long chatId = message.getChatId();
        final String originalText = message.getText();
        final String escapedText = escapeHashTags(originalText);
        
        sendMessageInternal(chatId, escapedText);
    }
}