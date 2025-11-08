package com.nb.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Interface for sending messages via a messaging platform.
 */
public interface MessageSender {

    /**
     * Executes the sending of a message.
     * @param message The message method to execute.
     * @return The sent Message object (can be null).
     * @throws TelegramApiException if the message cannot be sent.
     */
    Message execute(SendMessage message) throws TelegramApiException;
} 