package com.nb.service;

import com.nb.AbstractTest;
import io.micronaut.chatbots.telegram.api.Update;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.ToStringStyle;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceImplTest extends AbstractTest {

    @Inject
    private MessageService sut;


    @Test
    void testItWorks() throws IOException {
        final String givenMessage = getPostCommandJson().replaceAll("\n", "");
        final Update givenUpdate = jsonMapper.readValue(givenMessage, Update.class);

        sut.sendInputMessage(givenUpdate);

        final List<String> queueUrls = sqsClient.listQueues().queueUrls();
        final String givenQueueUrl = "http://sqs.us-east-1.localhost:4566/000000000000/test_input.fifo";
        assertTrue(queueUrls.contains(givenQueueUrl));
        final List<Message> actualMessages = getMessages(givenQueueUrl);
        assertFalse(actualMessages.isEmpty());
        assertEquals(1, actualMessages.size());
        final String actualMessage = actualMessages.get(0).body();
        final Update actualUpdate = jsonMapper.readValue(actualMessage, Update.class);
        assertEquals(actualUpdate.getUpdateId(), givenUpdate.getUpdateId());
        System.out.println(new String(new char[100]).replace('\0', '!'));
        System.out.println(ReflectionToStringBuilder.toString(actualUpdate, ToStringStyle.JSON_STYLE));
        System.out.println(new String(new char[100]).replace('\0', '!'));
        System.out.println(ReflectionToStringBuilder.toString(givenUpdate, ToStringStyle.JSON_STYLE));
        System.out.println(new String(new char[100]).replace('\0', '!'));
        Assertions.assertThat(actualUpdate.getMessage())
                .usingRecursiveComparison()
                .isEqualTo(givenUpdate.getMessage());
    }
}