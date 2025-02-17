package com.nb.config;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

@Singleton
public class SqsClientCreatedEventListener implements BeanCreatedEventListener<SqsClient> {

    private final String queueName;

    public SqsClientCreatedEventListener(@Value("${app.queue.input.name}") String queueName) {
        this.queueName = queueName;
    }

    @Override
    public SqsClient onCreated(BeanCreatedEvent<SqsClient> event) {
        SqsClient client = event.getBean();
        if (queueIsNotCreated(client)) {
            Map<QueueAttributeName, String> attributes = Map.of(
                    QueueAttributeName.FIFO_QUEUE, "true",
                    QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true"
            );
            final CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .attributes(attributes)
                    .queueName(queueName)
                    .build();
            client.createQueue(createQueueRequest);
        }
        return client;
    }

    private boolean queueIsNotCreated(SqsClient client) {
        return client.listQueues().queueUrls().stream().noneMatch(it -> it.contains(queueName));
    }
}
