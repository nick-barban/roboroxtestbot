package com.nb.service;

import com.nb.AbstractTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessageProducerTest extends AbstractTest {

    @Inject
    MessageProducer sut;

    @Test
    void shouldSendMessageSuccessfully() {
        final String givenQueueUrl = "http://sqs.us-east-1.localhost:4566/000000000000/test_input.fifo";
        final String givenMessage = UUID.randomUUID().toString();

        sut.sendInput(givenMessage, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final List<Message> actualMessages = getMessages(givenQueueUrl);
        assertFalse(actualMessages.isEmpty());
        assertEquals(1, actualMessages.size());
        final String actualMessage = actualMessages.get(0).body();
        org.assertj.core.api.Assertions.assertThat(actualMessage).isEqualTo(givenMessage);
    }
}