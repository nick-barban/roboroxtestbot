package com.nb.service;

import com.nb.config.TelegramConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Singleton
public class TelegramService extends TelegramLongPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramService.class);
    private final TelegramConfig config;

    public TelegramService(TelegramConfig config) {
        super(config.getBotToken());
        this.config = config;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // We don't need to handle incoming updates in this bot
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    private String escapeHashTags(String message) {
        if (message == null) {
            return null;
        }
        return message.replace("#", "\\#");
    }

    private void sendMessage(Long chatId, String message) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode(ParseMode.HTML);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Error sending message: {}", e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        LOG.debug("Telegram bot configuration:\n\ttoken={}\n\tusername={}", this.config.getBotToken(), this.config.getBotUsername());
        sendMessage(message.getChatId(), message.getText());
    }
}