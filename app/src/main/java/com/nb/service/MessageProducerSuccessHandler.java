package com.nb.service;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.jms.listener.JMSListenerSuccessHandler;
import jakarta.inject.Singleton;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MessageProducerSuccessHandler implements JMSListenerSuccessHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducerSuccessHandler.class);

    @Override
    public void handle(@NonNull Session session, @NonNull Message message) throws JMSException {
        LOG.debug("Successfully sent message: id={}, text={}", message.getJMSMessageID(), message.getBody(String.class));
    }
}
