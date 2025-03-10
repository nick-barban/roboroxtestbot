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
class FinalCommandHandler implements TelegramHandler<SendMessage> {

    private final SpaceParser<Update, Chat> spaceParser;

    FinalCommandHandler(SpaceParser<Update, Chat> spaceParser) {
        this.spaceParser = spaceParser;
    }

    @Override
    public boolean canHandle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        return true;
    }

    @Override
    public Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
        return SendMessageUtils.compose(
                spaceParser,
                input,
                "I don't know how to handle your query: %s%nUse next commands: /start, /about".formatted(input.getMessage().getText())
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
