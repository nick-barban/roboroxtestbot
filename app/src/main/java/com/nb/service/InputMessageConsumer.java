package com.nb.service;

import io.micronaut.messaging.annotation.MessageBody;
import io.micronaut.messaging.annotation.MessageHeader;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

//@JMSListener(CONNECTION_FACTORY_BEAN_NAME)
public class InputMessageConsumer {

    private static final Logger LOG = getLogger(InputMessageConsumer.class);

    private final AtomicInteger messageCount = getAtomicInteger();

    private static AtomicInteger getAtomicInteger() {
        LOG.debug("getAtomicInteger invoked");
        return new AtomicInteger(0);
    }

    //    @Queue(value = "${app.queue.input.name}", errorHandlers = InputMessageConsumerErroHandler.class)
    public void receive(@MessageBody String body, @MessageHeader("JMSXGroupID") String messageGroupId, @MessageHeader("JMS_SQS_DeduplicationId") String messageDeduplicationId) {
        LOG.debug("Message has been consumed. Message body: {}", body);
        int incremented = messageCount.incrementAndGet();
        LOG.debug("Message count: {}", incremented);
    }

    public int getMessageCount() {
        return messageCount.intValue();
    }
}