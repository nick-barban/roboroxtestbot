package com.nb;

import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.core.TextResourceLoader;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.core.CommandHandler;
import io.micronaut.chatbots.telegram.core.TelegramSlashCommandParser;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;

@Singleton
class AboutCommandHandler extends CommandHandler {

    private static final String COMMAND_ABOUT = "/about";

    AboutCommandHandler(
            TelegramSlashCommandParser slashCommandParser,
            TextResourceLoader textResourceLoader,
            SpaceParser<Update, Chat> spaceParser
    ) {
        super(slashCommandParser, textResourceLoader, spaceParser);
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND_ABOUT;
    }
}