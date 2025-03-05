package com.nb;

import io.micronaut.chatbots.core.Dispatcher;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.Send;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PostCommandHandlerTest extends AbstractTest {

    @Inject
    Dispatcher<TelegramBotConfiguration, Update, Send> dispatcher;

    @Inject
    JsonMapper jsonMapper;

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void postCommandHandlerExists() throws Exception {
        final Update input = jsonMapper.readValue(getPostCommandJson(), Update.class);
        Send send = dispatcher.dispatch(null, input).get();

        assertInstanceOf(SendMessage.class, send);
        assertEquals(expectedPostCommandResponse().trim(), ((SendMessage) send).getText().trim());
    }

    private String expectedPostCommandResponse() {
        return """
                Please send the post file that you want to publish. The file should be previously uploaded to S3.
                """;
    }
}