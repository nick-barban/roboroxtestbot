package com.nb;

import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.SendMessageUtils;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.chatbots.telegram.core.TelegramHandler;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

@Singleton
class HashCommandHandler implements TelegramHandler<SendMessage> {

    private final SpaceParser<Update, Chat> spaceParser;

    HashCommandHandler(SpaceParser<Update, Chat> spaceParser) {
        this.spaceParser = spaceParser;
    }

    @Override
    public boolean canHandle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        if (input.getMessage() == null || input.getMessage().getText() == null) {
            return false;
        }
        return input.getMessage().getText().startsWith("/hash");
    }

    @Override
    public Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        return SendMessageUtils.compose(
                spaceParser,
                input,
                "Please provide a text to hash."
        );
    }

    @Override
    public int getOrder() {
        return 3;
    }
} 