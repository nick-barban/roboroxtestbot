package com.nb;

import com.nb.service.MessageService;
import com.nb.service.PostService;
import com.nb.service.UserStateService;
import io.micronaut.chatbots.core.SpaceParser;
import io.micronaut.chatbots.core.TextResourceLoader;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.CommandHandler;
import io.micronaut.chatbots.telegram.core.SendMessageUtils;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.chatbots.telegram.core.TelegramSlashCommandParser;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Optional;

@Singleton
class PostCommandHandler extends CommandHandler {

    private static final String COMMAND_POST = "/post";
    private final MessageService messageService;
    private final UserStateService userStateService;
    private final PostService postService;
    private final SpaceParser<Update, Chat> spaceParser;

    PostCommandHandler(
            TelegramSlashCommandParser slashCommandParser,
            TextResourceLoader textResourceLoader,
            SpaceParser<Update, Chat> spaceParser,
            MessageService messageService,
            UserStateService userStateService,
            PostService postService
    ) {
        super(slashCommandParser, textResourceLoader, spaceParser);
        this.messageService = messageService;
        this.userStateService = userStateService;
        this.postService = postService;
        this.spaceParser = spaceParser;
    }

    @Override
    @NonNull
    public String getCommand() {
        return COMMAND_POST;
    }

    @Override
    public @NonNull Optional<SendMessage> handle(@Nullable TelegramBotConfiguration bot, @NonNull @NotNull Update input) {
        messageService.sendInputMessage(input);
        
        // Check if user is already in a post state
        Optional<String> currentState = userStateService.getPostState(input);
        
        if (currentState.isEmpty()) {
            // Start new post creation flow
            userStateService.setPostState(input);
            return SendMessageUtils.compose(
                    spaceParser,
                    input,
                    "Please send the post file that you want to publish. The file should be previously uploaded to S3."
            );
        }
        
        // Handle existing state
        return handleExistingState(input, currentState.get());
    }
    
    private Optional<SendMessage> handleExistingState(Update input, String state) {
        switch (state) {
            case "WAITING_FOR_FILE":
                // Handle file name input
                String fileName = input.getMessage().getText();
                userStateService.updatePostState(input, "WAITING_FOR_CHAT_ID", Map.of("fileName", fileName));
                return SendMessageUtils.compose(
                        spaceParser,
                        input,
                        "Please provide the chat ID where this post should be published."
                );
                        
            case "WAITING_FOR_CHAT_ID":
                // Handle chat ID input
                String chatId = input.getMessage().getText();
                Map<String, String> stateData = userStateService.getPostStateData(input);
                String postFileName = stateData.get("fileName");
                postService.savePostData(input, postFileName, chatId);
                userStateService.updatePostState(input, "COMPLETED", Map.of("chatId", chatId));
                userStateService.clearPostState(input);
                return SendMessageUtils.compose(
                        spaceParser,
                        input,
                        "Post configuration completed! The post will be published in the specified chat."
                );
                        
            default:
                // Reset state if something went wrong
                userStateService.clearPostState(input);
                return SendMessageUtils.compose(
                        spaceParser,
                        input,
                        "Something went wrong. Please start over with /post command."
                );
        }
    }
}