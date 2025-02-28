package com.nb.service;

import com.nb.config.TelegramConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
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

    private void sendMessage(String message, Long chatId) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Error sending message to Telegram", e);
        }
    }

    public void sendMessage(Message message) {
        sendMessage(message.getText(), message.getChatId());
    }
}