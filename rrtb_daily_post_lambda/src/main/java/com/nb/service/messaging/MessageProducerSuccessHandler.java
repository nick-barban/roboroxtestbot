package com.nb.service.messaging;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.jms.listener.JMSListenerSuccessHandler;
import jakarta.inject.Singleton;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class MessageProducerSuccessHandler implements JMSListenerSuccessHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProducerSuccessHandler.class);

    private final AtomicInteger messageCount = new AtomicInteger(0);

    @Override
    public void handle(@NonNull Session session, @NonNull Message message) throws JMSException {
        messageCount.incrementAndGet();
        LOG.info("Successfully sent message: id={}, text={}", message.getJMSMessageID(), message.getBody(String.class));
    }

    public int getMessageCount() {
        return messageCount.intValue();
    }
}
