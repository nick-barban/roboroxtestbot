package com.nb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.chatbots.telegram.api.Update;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MessageServiceImpl implements MessageService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageServiceImpl.class);
    private final InputMessageProducer inputMessageProducer;

    public MessageServiceImpl(InputMessageProducer inputMessageProducer) {
        this.inputMessageProducer = inputMessageProducer;
    }

    @Override
    public void sendInputMessage(Update input) {
        final String msg;
        try {
            msg = new ObjectMapper().writeValueAsString(input);
            final Long chatId = input.getMessage().getChat().getId();
            final Integer updateId = input.getUpdateId();
            LOG.info("SendMessage with id: {} and updateID: {} from chat: {}", input.getMessage().getMessageId(), updateId, chatId);
            inputMessageProducer.send(msg, String.valueOf(chatId), String.valueOf(updateId));
        } catch (Exception e) {
            throw new RuntimeException("Could not send message to queue", e);
        }
    }
}
