package com.nb;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.nb.repository.ChatRepository;
import com.nb.service.FileService;
import com.nb.service.messaging.MessageProducer;
import io.micronaut.chatbots.telegram.api.Chat;
import io.micronaut.chatbots.telegram.api.Message;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.function.aws.MicronautRequestHandler;
import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

@SerdeImport(ScheduledEvent.class)
public class SchedulerHandler extends MicronautRequestHandler<ScheduledEvent, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerHandler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy",
            new Locale("uk", "UA"));
    private static final String DATE_PLACEHOLDER = "{{date}}";

    @Inject
    private FileService fileService;
    @Inject
    private MessageProducer producer;
    @Inject
    private JsonMapper objectMapper;
    @Inject
    private ChatRepository chatRepository;

    @Value("${app.chat-id-from-header}")
    private boolean chatIdFromHeader;

    @Value("${app.mode}")
    private String mode;

    @Override
    public Void execute(ScheduledEvent input) {
        LOG.info("Scheduled handler invocation");

        final Map<String, String> posts = fileService.readTodayPosts();
        LOG.info("Read {} posts for today", posts.size());

        posts.forEach((name, post) -> {
            if (StringUtils.isEmpty(post)) {
                LOG.warn("No post for name {}", name);
            } else {
                try {
                    final Chat chat = getChat(name, post);
                    sendPost(chat, post);
                } catch (Exception e) {
                    LOG.error("Could not send post: %s as could not obtain chat: {}".formatted(name), e);
                }
            }
        });

        return null;
    }

    private String getChatName(String text) {
        String[] split = text.split("\n");
        return Arrays.stream(split)
                .filter(line -> line.startsWith("#telegramGroup:"))
                .findFirst()
                .map(line -> line.split(":")[1].trim())
                .orElseThrow();
    }

    private void sendPost(Chat chat, @NonNull String text) {
        try {
            String processedText = text.replace(DATE_PLACEHOLDER, LocalDate.now().format(DATE_FORMATTER));
            final Update update = new Update();
            final Message message = new Message();
            message.setChat(chat);
            
            if (mode == "prod") {
            final String body = getBody(processedText);
                message.setText(body);
            } else {
                message.setText(processedText);
            }

            update.setMessage(message);
            final String msg = objectMapper.writeValueAsString(update);
            producer.sendOutput(msg, chat.getTitle(), "[%s]%s".formatted(chat.getTitle(), LocalDate.now()));
            LOG.info("Send post: {} to output queue", chat.getTitle());
        } catch (Exception e) {
            LOG.error("Could not send post: %s".formatted(chat.getTitle()), e);
        }
    }

    private String getBody(String text) {
        String[] split = text.split("#");
        return Arrays.stream(split)
                .filter(line -> line.startsWith("#body:"))
                .findFirst()
                .map(line -> line.split(":")[1].trim())
                .orElseThrow();
    }

    private Long getChatId(String text) throws Exception {
        String[] split = text.split("#");
        return Arrays.stream(split)
                .filter(line -> line.startsWith("#chatId:"))
                .findFirst()
                .map(line -> line.split(":")[1].trim())
                .map(Long::parseLong)
                .orElseThrow();
    }

    private Chat getChat(String postName, String post) throws Exception {
        if (chatIdFromHeader) {
            Long chatId = getChatId(post);
            final Chat chat = new Chat();
            chat.setId(chatId);
            String chatName = getChatName(post);
            chat.setTitle(chatName);
            chat.setType("supergroup");
            return chat;
        } else {
            final String chatName = postName.split("_")[0];
            return chatRepository.getChatByName(chatName).orElseThrow(Exception::new);
        }
    }
}
