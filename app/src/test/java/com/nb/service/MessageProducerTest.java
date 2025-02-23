package com.nb.service;

import com.nb.AbstractTest;
import jakarta.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessageProducerTest extends AbstractTest {

    @Inject
    InputMessageProducer sut;

    @Test
    void testItWorks() throws IOException {
        final String givenMessage = UUID.randomUUID().toString();

        sut.send(givenMessage, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final String givenQueueUrl = "http://sqs.us-east-1.localhost:4566/000000000000/test_input.fifo";
        final List<Message> actualMessages = getMessages(givenQueueUrl);
        assertFalse(actualMessages.isEmpty());
        assertEquals(1, actualMessages.size());
        final String actualMessage = actualMessages.get(0).body();
        MatcherAssert.assertThat(actualMessage, samePropertyValuesAs(givenMessage));
    }
}