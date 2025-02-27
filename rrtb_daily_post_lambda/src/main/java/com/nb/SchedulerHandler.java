package com.nb;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.nb.repository.ChatRepository;
import com.nb.service.FileService;
import com.nb.service.messaging.MessageProducer;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Message;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.function.aws.MicronautRequestHandler;
import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@SerdeImport(ScheduledEvent.class)
public class SchedulerHandler extends MicronautRequestHandler<ScheduledEvent, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerHandler.class);

    @Inject
    private FileService fileService;
    @Inject
    private MessageProducer producer;
    @Inject
    private JsonMapper objectMapper;
    @Inject
    private ChatRepository chatRepository;

    @Override
    public Void execute(ScheduledEvent input) {
        LOG.info("Scheduled handler invocation");

        final Map<String, String> posts = fileService.readPosts();
        LOG.info("Read {} posts", posts.size());

        posts.forEach((name, post) -> {
            if (StringUtils.isEmpty(post)) {
                LOG.warn("No post for name {}", name);
            } else {
                final Long chatId;
                try {
                    chatId = getChatId(name);
                    sendPost(name, chatId, post);
                } catch (Exception e) {
                    LOG.error("Could not send post: %s as could not obtain chatId: {}".formatted(name), e);
                }
            }
        });

        return null;
    }

    private void sendPost(@NonNull String name, @NonNull Long chatId, @NonNull String text) {
        try {
            final Update update = new Update();
            final Message message = new Message();
            final Chat chat = new Chat();
            chat.setId(chatId);
            message.setChat(chat);
            message.setText(text);
            update.setMessage(message);
            final String msg = objectMapper.writeValueAsString(update);
            producer.sendOutput(msg, String.valueOf(chatId), UUID.randomUUID().toString());
            LOG.info("Send post: {} for chat: {} to output queue", name, chatId);
        } catch (IOException e) {
            LOG.error("Could not send post: %s for chat: %d".formatted(name, chatId), e);
        }
    }

    private Long getChatId(String postName) throws Exception {
        final String chatName = postName.split("_")[0];
        return chatRepository.getChatIdByName(chatName)
                .orElseThrow(Exception::new);
    }
}
