package com.nb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.util.List;
import java.util.Map;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
class AppStackTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(DYNAMODB, LAMBDA, S3, SQS, CLOUDWATCH);

    @BeforeAll
    static void beforeAll() {
        // Set AWS credentials and region for LocalStack
        System.setProperty("aws.accessKeyId", localStack.getAccessKey());
        System.setProperty("aws.secretKey", localStack.getSecretKey());
        System.setProperty("aws.region", localStack.getRegion());
    }

    @Test
    void testDynamoDBTables() {
        final App app = new App();
        final AppStack stack = new AppStack(app, "TestStack");
        final Template template = Template.fromStack(stack);

        // Test School table
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
                "TableName", "School",
                "KeySchema", List.of(Map.of(
                        "AttributeName", "id",
                        "KeyType", "HASH"
                )),
                "AttributeDefinitions", List.of(Map.of(
                        "AttributeName", "id",
                        "AttributeType", "S"
                )),
                "BillingMode", "PAY_PER_REQUEST",
                "PointInTimeRecoverySpecification", Map.of(
                        "PointInTimeRecoveryEnabled", true
                )
        ))).doesNotThrowAnyException();

        // Test Group table
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
                "TableName", "Group",
                "KeySchema", List.of(
                        Map.of("AttributeName", "id", "KeyType", "HASH"),
                        Map.of("AttributeName", "schoolId", "KeyType", "RANGE")
                ),
                "AttributeDefinitions", List.of(
                        Map.of("AttributeName", "id", "AttributeType", "S"),
                        Map.of("AttributeName", "schoolId", "AttributeType", "S"),
                        Map.of("AttributeName", "teacherId", "AttributeType", "S")
                ),
                "BillingMode", "PAY_PER_REQUEST",
                "PointInTimeRecoverySpecification", Map.of(
                        "PointInTimeRecoveryEnabled", true
                )
        ))).doesNotThrowAnyException();

        // Test User table
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
                "TableName", "User",
                "KeySchema", List.of(
                        Map.of("AttributeName", "id", "KeyType", "HASH"),
                        Map.of("AttributeName", "type", "KeyType", "RANGE")
                ),
                "AttributeDefinitions", List.of(
                        Map.of("AttributeName", "id", "AttributeType", "S"),
                        Map.of("AttributeName", "type", "AttributeType", "S"),
                        Map.of("AttributeName", "parentId", "AttributeType", "S")
                ),
                "BillingMode", "PAY_PER_REQUEST",
                "PointInTimeRecoverySpecification", Map.of(
                        "PointInTimeRecoveryEnabled", true
                )
        ))).doesNotThrowAnyException();

        // Test UserState table
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
                "TableName", "UserState",
                "KeySchema", List.of(
                        Map.of("AttributeName", "userId", "KeyType", "HASH"),
                        Map.of("AttributeName", "commandType", "KeyType", "RANGE")
                ),
                "AttributeDefinitions", List.of(
                        Map.of("AttributeName", "userId", "AttributeType", "S"),
                        Map.of("AttributeName", "commandType", "AttributeType", "S")
                ),
                "BillingMode", "PAY_PER_REQUEST",
                "PointInTimeRecoverySpecification", Map.of(
                        "PointInTimeRecoveryEnabled", true
                ),
                "TimeToLiveSpecification", Map.of(
                        "AttributeName", "ttl",
                        "Enabled", true
                )
        ))).doesNotThrowAnyException();
    }

    @Test
    void testLambdaFunctions() {
        final App app = new App();
        final AppStack stack = new AppStack(app, "TestStack");
        final Template template = Template.fromStack(stack);

        // Test Input Lambda
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "io.micronaut.chatbots.telegram.lambda.Handler",
                "Runtime", "java17",
                "Timeout", 10,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ))).doesNotThrowAnyException();

        // Test Daily Post Lambda
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "com.nb.SchedulerHandler",
                "Runtime", "java17",
                "Timeout", 20,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ))).doesNotThrowAnyException();

        // Test Output Lambda
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "com.nb.FunctionRequestHandler",
                "Runtime", "java17",
                "Timeout", 10,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ))).doesNotThrowAnyException();
    }

    @Test
    void testSQSQueues() {
        final App app = new App();
        final AppStack stack = new AppStack(app, "TestStack");
        final Template template = Template.fromStack(stack);

        // Test Input Queue
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "rrtb_input.fifo",
                "FifoQueue", true
        ))).doesNotThrowAnyException();

        // Test Input DLQ
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "dlq_rrtb_input.fifo",
                "FifoQueue", true
        ))).doesNotThrowAnyException();

        // Test Output Queue
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "rrtb_output.fifo",
                "FifoQueue", true
        ))).doesNotThrowAnyException();

        // Test Output DLQ
        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "dlq_rrtb_output.fifo",
                "FifoQueue", true
        ))).doesNotThrowAnyException();
    }

    @Test
    void testS3Bucket() {
        final App app = new App();
        final AppStack stack = new AppStack(app, "TestStack");
        final Template template = Template.fromStack(stack);

        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::S3::Bucket", Map.of(
                "PublicAccessBlockConfiguration", Map.of(
                        "BlockPublicAcls", true,
                        "BlockPublicPolicy", true,
                        "IgnorePublicAcls", true,
                        "RestrictPublicBuckets", true
                ),
                "VersioningConfiguration", Map.of(
                        "Status", "Enabled"
                )
        ))).doesNotThrowAnyException();
    }

    @Test
    void testCloudWatchEvents() {
        final App app = new App();
        final AppStack stack = new AppStack(app, "TestStack");
        final Template template = Template.fromStack(stack);

        Assertions.assertThatCode(() -> template.hasResourceProperties("AWS::Events::Rule", Map.of(
                "ScheduleExpression", "cron(0 7 * * ? *)",
                "State", "ENABLED"
        ))).doesNotThrowAnyException();
    }
}
