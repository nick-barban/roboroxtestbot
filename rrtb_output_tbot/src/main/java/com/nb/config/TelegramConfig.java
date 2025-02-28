package com.nb.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;

@Context
@ConfigurationProperties("telegram")
public class TelegramConfig {
    private String botToken;
    private String botUsername;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }
}