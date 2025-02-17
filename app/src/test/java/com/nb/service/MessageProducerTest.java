package com.nb.service;

import com.nb.AbstractTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageProducerTest extends AbstractTest {

    @Inject
    InputMessageProducer sut;

    @Inject
    InputMessageConsumer consumer;


    @Test
    void testItWorks() {
        assertEquals(0, consumer.getMessageCount());
        final String givenMessage = UUID.randomUUID().toString();

        sut.send(givenMessage, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        await().until(() -> consumer.getMessageCount(), equalTo(1));
        assertEquals(1, consumer.getMessageCount());
    }
}