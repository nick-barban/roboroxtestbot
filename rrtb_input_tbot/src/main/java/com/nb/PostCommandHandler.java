package com.nb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nb.service.MessageService;
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
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
class PostCommandHandler extends CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PostCommandHandler.class);
    private static final String COMMAND_POST = "/post";
    private final MessageService service;
    private final ObjectMapper objectMapper;

    PostCommandHandler(
            TelegramSlashCommandParser slashCommandParser,
            TextResourceLoader textResourceLoader,
            SpaceParser<Update, Chat> spaceParser,
            MessageService service
    ) {
        super(slashCommandParser, textResourceLoader, spaceParser);
        this.service = service;
        // TODO by nickbarban: 17/02/25 Should be initialized in a central config bean
        objectMapper = new ObjectMapper();
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND_POST;
    }

    @Override
    public @NonNull Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NonNull @NotNull Update input) {
        service.sendInputMessage(input);
        return super.handle(bot, input);
    }
}