package com.nb;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.nb.service.TelegramService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.function.aws.MicronautRequestHandler;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

@SuppressWarnings({"java:S6813", "unused"})
public class FunctionRequestHandler extends MicronautRequestHandler<SQSEvent, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Inject
    private TelegramService telegramService;
    @Inject
    private JsonMapper objectMapper;

    @Override
    public Void execute(@Nullable SQSEvent event) {
        if (event != null && event.getRecords() != null) {
            for (SQSEvent.SQSMessage message : event.getRecords()) {
                LOG.info("Processing SQS message with ID: {}", message.getMessageId());
                LOG.info("Message body: {}", message.getBody());
                // Add your message processing logic here
                try {
                    final Update update = objectMapper.readValue(message.getBody(), Update.class);
                    telegramService.sendMessage(update.getMessage());
                    LOG.info("Message {} was sent to chat: {}", update.getMessage().getMessageId(), update.getMessage().getChat().getId());
                } catch (IOException e) {
                    LOG.error("Error processing message: %s".formatted(message.getMessageId()), e);
                }
            }
        }
        return null;
    }
} 