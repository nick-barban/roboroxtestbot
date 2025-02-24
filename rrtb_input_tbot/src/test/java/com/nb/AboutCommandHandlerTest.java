package com.nb;

import io.micronaut.chatbots.core.Dispatcher;
import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.chatbots.telegram.api.send.Send;
import io.micronaut.chatbots.telegram.api.send.SendMessage;
import io.micronaut.chatbots.telegram.core.TelegramBotConfiguration;
import io.micronaut.context.BeanContext;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutCommandHandlerTest extends AbstractTest {

    @Inject
    BeanContext ctx;

    @Inject
    Dispatcher<TelegramBotConfiguration, Update, Send> dispatcher;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void beanOfTypeHelloWorldHandlerExists() {
        assertTrue(ctx.containsBean(AboutCommandHandler.class));
    }

    @Test
    void aboutCommandHandlerExists() throws Exception {
        Update input = jsonMapper.readValue(getAboutCommandJson(), Update.class);
        Send send = dispatcher.dispatch(null, input).get();

        assertTrue(send instanceof SendMessage);
        assertEquals("Telegram Bot developed with HEART and [Micronaut](https://micronaut.io)", ((SendMessage) send).getText().trim());
    }

    private String getAboutCommandJson() throws IOException {
        return new String(AboutCommandHandlerTest.class.getResourceAsStream("/mockAboutCommand.json").readAllBytes());
    }
}