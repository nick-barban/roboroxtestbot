package com.nb.service.messaging;

import io.micronaut.jms.annotations.JMSProducer;
import io.micronaut.jms.annotations.Queue;
import io.micronaut.messaging.annotation.MessageBody;
import io.micronaut.messaging.annotation.MessageHeader;

import static io.micronaut.jms.sqs.configuration.SqsConfiguration.CONNECTION_FACTORY_BEAN_NAME;

@JMSProducer(CONNECTION_FACTORY_BEAN_NAME)
public interface DlqMessageProducer {

    @Queue("${app.queue.input.dlq}")
    void send(@MessageBody String body, @MessageHeader("JMSXGroupID") String messageGroupId, @MessageHeader("JMS_SQS_DeduplicationId") String messageDeduplicationId);

    default String getQueueName() {
        return System.getProperty("app.queue.input.dlq");
    }
}
