package com.nb;

import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.SendMessageUtils;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.chatbots.telegram.core.TelegramHandler;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class HashCommandHandler implements TelegramHandler<SendMessage> {
    private static final Logger log = LoggerFactory.getLogger(HashCommandHandler.class);
    private final SpaceParser<Update, Chat> spaceParser;
        private final SchoolService schoolService;
    
        HashCommandHandler(SpaceParser<Update, Chat> spaceParser, SchoolService schoolService) {
                    this.spaceParser = spaceParser;
                    this.schoolService = schoolService;
        }
    
        @Override
        public boolean canHandle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
            if (input.getMessage() == null || input.getMessage().getText() == null) {
                return false;
            }
            return input.getMessage().getText().startsWith("#");
        }
    
        @Override
        public Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NotNull Update input) {
            @Nullable
            final String text = input.getMessage().getText();
            String[] split = text.split("\n");
            String firstLine = split[0];
            RrtbHashCommand command = RrtbHashCommand.valueOf(firstLine);
    
            switch (command) {
                case ADD_SCHOOL:
                    log.info("Adding school");
                    schoolService.addSchool(text);
                break;
        
            default:
                break;
        }
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