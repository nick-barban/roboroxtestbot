package com.nb;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@MicronautTest(rebuildContext = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTest implements TestPropertyProvider {
    protected static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");
    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(LocalStackContainer.Service.SQS);
    public static final String GIVEN_TEST_INPUT_FIFO_QUEUE_NAME = "test_input.fifo";
    public static final String GIVEN_TEST_OUTPUT_FIFO_QUEUE_NAME = "test_output.fifo";

    @Inject
    protected JsonMapper jsonMapper;

    @Inject
    protected SqsClient sqsClient;

    @Override
    public @NonNull Map<String, String> getProperties() {
        if (!LOCALSTACK.isRunning()) {
            LOCALSTACK.start();
        }
        return Map.of(
                "app.queue.input.name", GIVEN_TEST_INPUT_FIFO_QUEUE_NAME,
                "app.queue.output.name", GIVEN_TEST_OUTPUT_FIFO_QUEUE_NAME,
                "aws.access-key-id", LOCALSTACK.getAccessKey(),
                "aws.secret-key", LOCALSTACK.getSecretKey(),
                "aws.region", LOCALSTACK.getRegion(),
                "aws.services.sqs.endpoint-override", LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString()
        );
    }

    protected String getPostCommandJson() throws IOException {
        return new String(PostCommandHandlerTest.class.getResourceAsStream("/mockPostCommand.json").readAllBytes());
    }

    protected List<Message> getMessages(String givenQueueUrl) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(givenQueueUrl)
                .build();
        final List<Message> actualMessages = sqsClient.receiveMessage(request).messages();
        return actualMessages;
    }

    @AfterEach
    public void cleanup() {
        final PurgeQueueRequest request = PurgeQueueRequest.builder().queueUrl(GIVEN_TEST_INPUT_FIFO_QUEUE_NAME).build();
        final PurgeQueueResponse response = sqsClient.purgeQueue(request);
        System.out.println(new String(new char[5]).replaceAll("\0", "[cleanup]".toUpperCase()));
        Assertions.assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
    }
}
