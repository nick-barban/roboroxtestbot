package com.nb;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.function.aws.MicronautRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionRequestHandler extends MicronautRequestHandler<SQSEvent, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Override
    public Void execute(@Nullable SQSEvent event) {
        if (event != null && event.getRecords() != null) {
            for (SQSEvent.SQSMessage message : event.getRecords()) {
                LOG.info("Processing SQS message with ID: {}", message.getMessageId());
                LOG.info("Message body: {}", message.getBody());
                // Add your message processing logic here
            }
        }
        return null;
    }
} 