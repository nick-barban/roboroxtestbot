package com.nb.service;

import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class MessageServiceImpl implements MessageService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageServiceImpl.class);
    private final MessageProducer inputMessageProducer;
    private final JsonMapper mapper;

    public MessageServiceImpl(MessageProducer inputMessageProducer, JsonMapper mapper) {
        this.inputMessageProducer = inputMessageProducer;
        this.mapper = mapper;
    }

    @Override
    public void sendInputMessage(Update input) {
        final Long chatId = input.getMessage().getChat().getId();
        final Integer updateId = input.getUpdateId();
        final Integer messageId = input.getMessage().getMessageId();

        try {
            final String msg = mapper.writeValueAsString(input);
            LOG.debug("Handle next message: {}", msg);
            sendInputMessage(updateId, chatId, messageId, msg);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing input message: %s".formatted(input.getUpdateId()), e);
        }
    }

    private void sendInputMessage(Integer updateId, Long chatId, Integer messageId, String msg) {
        try {
            LOG.info("SendMessage with id: {} and updateID: {} from chat: {}", messageId, updateId, chatId);
            inputMessageProducer.sendInput(msg, String.valueOf(chatId), String.valueOf(updateId));
        } catch (Exception e) {
            throw new RuntimeException("Could not send message to queue", e);
        }
    }
}
