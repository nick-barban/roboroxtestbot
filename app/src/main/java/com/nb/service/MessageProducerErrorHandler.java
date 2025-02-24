package com.nb.service;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.jms.listener.JMSListenerErrorHandler;
import jakarta.inject.Singleton;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MessageProducerErrorHandler implements JMSListenerErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducerErrorHandler.class);

    @Override
    public void handle(@NonNull Session session, @NonNull Message message, @NonNull Throwable ex) {
        try {
            LOG.error("Error occurred when try to send message: id=%s, text=%s".formatted(message.getJMSMessageID(), message.getBody(String.class)), ex);
        } catch (JMSException e) {
            LOG.error("Error occurred when try to send message. Could not extract JMS message id when created error message.", e);
            throw new RuntimeException(e);
        }
    }
}
