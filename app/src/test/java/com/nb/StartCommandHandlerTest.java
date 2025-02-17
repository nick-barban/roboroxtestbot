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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@MicronautTest(startApplication = false)
class StartCommandHandlerTest extends AbstractTest {

    @Inject
    BeanContext ctx;

    @Inject
    Dispatcher<TelegramBotConfiguration, Update, Send> dispatcher;

    @Inject
    JsonMapper jsonMapper;

//    @Inject
//    MessageProcessor processor;

    @Test
    void beanOfTypeStartCommandHandlerExists() {
        assertTrue(ctx.containsBean(StartCommandHandler.class));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void startCommandHandlerExists() throws Exception {
        final Update input = jsonMapper.readValue(getStartCommandJson(), Update.class);
        Send send = dispatcher.dispatch(null, input).get();

        assertInstanceOf(SendMessage.class, send);
        assertEquals(expectedStartCommandResponse().trim(), ((SendMessage) send).getText().trim());
    }

    private String expectedStartCommandResponse() {
        return """
                You can use next commands:
                /schedule - get schedule per week
                /post - generate post about today's class in the hosting RoboRox channel
                
                Nearest future commands:
                /listschools - list all RoboRox schools
                /uad - get info about RoboRoxAkademiaDytynstva
                /azbuka - get info about RoboRoxAzbuka
                /fenix - get info about RoboRoxFenix
                /lk - get info about RoboRoxLovelyKids
                /vg - get info about RoboRoxVilaGema
                /synergy - get info about RoboRoxSynergy
                /vh - get info about RoboRoxVeselaHata
                /leleka - get info about RoboRoxLeleka
                /weekend - get info about RoboRoxWeekend
                /camp - get info about RoboRoxCamp
                /teachers - get info about RoboRox teachers
                /logistics - get today's driver's route description
                """;
    }

    private String getStartCommandJson() throws IOException {
        return new String(StartCommandHandlerTest.class.getResourceAsStream("/mockStartCommand.json").readAllBytes());
    }
}