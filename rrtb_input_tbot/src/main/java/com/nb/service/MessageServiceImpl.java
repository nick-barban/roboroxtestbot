package com.nb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        if (LOG.isDebugEnabled()){
            try {
                LOG.debug("Handle next message: {}", mapper.writeValueAsBytes(input));
            } catch (IOException e) {
                LOG.error("Error serializing input message: %s".formatted(input.getUpdateId()), e);
            }
        }

        final String msg;
        try {
            msg = new ObjectMapper().writeValueAsString(input);
            final Long chatId = input.getMessage().getChat().getId();
            final Integer updateId = input.getUpdateId();
            LOG.info("SendMessage with id: {} and updateID: {} from chat: {}", input.getMessage().getMessageId(), updateId, chatId);
            inputMessageProducer.sendInput(msg, String.valueOf(chatId), String.valueOf(updateId));
        } catch (Exception e) {
            throw new RuntimeException("Could not send message to queue", e);
        }
    }
}
