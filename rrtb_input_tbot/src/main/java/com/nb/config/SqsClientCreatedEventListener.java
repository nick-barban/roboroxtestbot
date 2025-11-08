package com.nb.config;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;
import java.util.stream.Stream;

@Singleton
public class SqsClientCreatedEventListener implements BeanCreatedEventListener<SqsClient> {

    private static final Logger LOG = LoggerFactory.getLogger(SqsClientCreatedEventListener.class);

    private final AppConfig.AppQueue input;
    private final AppConfig.AppQueue output;
    private SqsClient client;

    public SqsClientCreatedEventListener(AppConfig appConfig) {
        this.input = appConfig.getInput();
        this.output = appConfig.getOutput();
    }

    @Override
    public SqsClient onCreated(BeanCreatedEvent<SqsClient> event) {
        this.client = event.getBean();
        Stream.of(input, output)
                .filter(this::queueIsNotCreated)
                .forEach(this::createQueue);
        return client;
    }

    private void createQueue(AppConfig.AppQueue queue) {
        createQueue(queue.getName());

        if (StringUtils.isNotEmpty(queue.getDlq())) {
            createQueue(queue.getDlq());
        }
    }

    private void createQueue(String queueName) {
        final Map<QueueAttributeName, String> attributes = Map.of(
                QueueAttributeName.FIFO_QUEUE, "true",
                QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true"
        );
        final CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .attributes(attributes)
                .queueName(queueName)
                .build();
        final CreateQueueResponse response = this.client.createQueue(createQueueRequest);
        LOG.info("Queue created: {}", response.queueUrl());
    }

    private boolean queueIsNotCreated(AppConfig.AppQueue queue) {
        return this.client.listQueues().queueUrls().stream().noneMatch(it -> it.contains(queue.getName()));
    }
}
