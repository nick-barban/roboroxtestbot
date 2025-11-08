package com.nb;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
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
    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.DYNAMODB);
    public static final String GIVEN_TEST_INPUT_FIFO_QUEUE_NAME = "test_input.fifo";
    public static final String GIVEN_TEST_OUTPUT_FIFO_QUEUE_NAME = "test_output.fifo";
    public static final String USER_STATE_TABLE_NAME = "UserState";
    public static final String GROUP_TABLE_NAME = "GroupTable";
    public static final String SCHOOL_TABLE_NAME = "School";

    @Inject
    protected JsonMapper jsonMapper;

    @Inject
    protected SqsClient sqsClient;

    @Inject
    protected DynamoDbClient dynamoDbClient;

    @BeforeAll
    public void setupDynamoDB() {
        if (!LOCALSTACK.isRunning()) {
            LOCALSTACK.start();
        }

        // Create UserStateTable
        CreateTableRequest userStateTableRequest = CreateTableRequest.builder()
                .tableName(USER_STATE_TABLE_NAME)
                .keySchema(
                    KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build(),
                    KeySchemaElement.builder().attributeName("commandType").keyType(KeyType.RANGE).build()
                )
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("commandType").attributeType(ScalarAttributeType.S).build()
                )
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
                .build();

        // Create GroupTable
        CreateTableRequest groupTableRequest = CreateTableRequest.builder()
                .tableName(GROUP_TABLE_NAME)
                .keySchema(
                    KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()
                )
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                )
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
                .build();
                
        // Create SchoolTable
        CreateTableRequest schoolTableRequest = CreateTableRequest.builder()
                .tableName(SCHOOL_TABLE_NAME)
                .keySchema(
                    KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()
                )
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                )
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
                .build();

        try {
            dynamoDbClient.createTable(userStateTableRequest);
            dynamoDbClient.createTable(groupTableRequest);
            dynamoDbClient.createTable(schoolTableRequest);
        } catch (ResourceInUseException e) {
            // Tables already exist, ignore
        }
    }

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
                "aws.services.sqs.endpoint-override", LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString(),
                "aws.services.dynamodb.endpoint-override", LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()
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
