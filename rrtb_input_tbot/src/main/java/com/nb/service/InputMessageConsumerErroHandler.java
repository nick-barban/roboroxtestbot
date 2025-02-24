package com.nb.service;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.jms.listener.JMSListenerErrorHandler;
import jakarta.inject.Singleton;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

@Singleton
public class InputMessageConsumerErroHandler implements JMSListenerErrorHandler {

    private final DlqMessageProducer producer;

    public InputMessageConsumerErroHandler(DlqMessageProducer producer) {
        this.producer = producer;
    }

    @Override
    public void handle(@NonNull Session session, @NonNull Message message, @NonNull Throwable ex) {
        try {
            producer.send(message.getBody(String.class), message.getJMSCorrelationID(), message.getJMSMessageID());
        } catch (JMSException e) {
            String error = "Cannot send message to %s".formatted(producer.getQueueName());
            throw new RuntimeException(error, e);
        }
    }
}
