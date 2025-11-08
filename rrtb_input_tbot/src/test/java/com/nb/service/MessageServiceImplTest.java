package com.nb.service;

import com.nb.AbstractTest;
import io.micronaut.chatbots.telegram.api.Update;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceImplTest extends AbstractTest {

    @Inject
    private MessageService sut;


    @Test
    void testItWorks() throws IOException {
        final String givenMessage = getPostCommandJson().replaceAll("\n", "");
        final Update givenUpdate = jsonMapper.readValue(givenMessage, Update.class);

        sut.sendInputMessage(givenUpdate);

        final String givenQueueUrl = "http://sqs.us-east-1.localhost:4566/000000000000/test_input.fifo";
        final List<Message> actualMessages = getMessages(givenQueueUrl);
        Assertions.assertThat(actualMessages).hasSize(1);
        final String actualMessage = actualMessages.get(0).body();
        final Update actualUpdate = jsonMapper.readValue(actualMessage, Update.class);
        assertEquals(actualUpdate.getUpdateId(), givenUpdate.getUpdateId());
        Assertions.assertThat(actualUpdate.getMessage())
                .usingRecursiveComparison()
                .isEqualTo(givenUpdate.getMessage());
    }
}