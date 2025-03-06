package com.nb;

import com.nb.domain.RrtbCommand;
import com.nb.service.messaging.MessageProducer;
import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.core.TextResourceLoader;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.CommandHandler;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.chatbots.telegram.core.TelegramSlashCommandParser;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
class StartCommandHandler extends CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StartCommandHandler.class);
    private final MessageProducer producer;
    private final JsonMapper objectMapper;

    StartCommandHandler(
            TelegramSlashCommandParser slashCommandParser,
            TextResourceLoader textResourceLoader,
            SpaceParser<Update, Chat> spaceParser,
            MessageProducer producer, JsonMapper objectMapper
    ) {
        super(slashCommandParser, textResourceLoader, spaceParser);
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public String getCommand() {
        return RrtbCommand.START.command();
    }

    @Override
    public @NonNull Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NonNull @NotNull Update input) {
        try {
            final String msg = objectMapper.writeValueAsString(input);
            final Long chatId = input.getMessage().getChat().getId();
            final Integer updateId = input.getUpdateId();
            LOG.info("SendMessage with id: {} and updateID: {} from chat: {}", input.getMessage().getMessageId(), updateId, chatId);
            producer.sendInput(msg, String.valueOf(chatId), String.valueOf(updateId));
            return super.handle(bot, input);
        } catch (Exception e) {
            throw new RuntimeException("Could not send message to queue", e);
        }
    }
}