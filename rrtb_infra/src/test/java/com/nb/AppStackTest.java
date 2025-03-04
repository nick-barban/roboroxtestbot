package com.nb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

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
        App app = new App();
        AppStack stack = new AppStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Test School table
        template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
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
        ));

        // Test Group table
        template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
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
        ));

        // Test User table
        template.hasResourceProperties("AWS::DynamoDB::Table", Map.of(
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
        ));
    }

    @Test
    void testLambdaFunctions() {
        App app = new App();
        AppStack stack = new AppStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Test Input Lambda
        template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "io.micronaut.chatbots.telegram.lambda.Handler",
                "Runtime", "java17",
                "Timeout", 10,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ));

        // Test Daily Post Lambda
        template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "com.nb.SchedulerHandler",
                "Runtime", "java17",
                "Timeout", 20,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ));

        // Test Output Lambda
        template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "com.nb.FunctionRequestHandler",
                "Runtime", "java17",
                "Timeout", 10,
                "MemorySize", 256,
                "Architectures", List.of("arm64"),
                "TracingConfig", Map.of(
                        "Mode", "Active"
                )
        ));
    }

    @Test
    void testSQSQueues() {
        App app = new App();
        AppStack stack = new AppStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Test Input Queue
        template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "rrtb_input.fifo",
                "FifoQueue", true
        ));

        // Test Input DLQ
        template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "dlq_rrtb_input.fifo",
                "FifoQueue", true
        ));

        // Test Output Queue
        template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "rrtb_output.fifo",
                "FifoQueue", true
        ));

        // Test Output DLQ
        template.hasResourceProperties("AWS::SQS::Queue", Map.of(
                "QueueName", "dlq_rrtb_output.fifo",
                "FifoQueue", true
        ));
    }

    @Test
    void testS3Bucket() {
        App app = new App();
        AppStack stack = new AppStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::S3::Bucket", Map.of(
                "PublicAccessBlockConfiguration", Map.of(
                        "BlockPublicAcls", true,
                        "BlockPublicPolicy", true,
                        "IgnorePublicAcls", true,
                        "RestrictPublicBuckets", true
                ),
                "VersioningConfiguration", Map.of(
                        "Status", "Enabled"
                )
        ));
    }

    @Test
    void testCloudWatchEvents() {
        App app = new App();
        AppStack stack = new AppStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::Events::Rule", Map.of(
                "ScheduleExpression", "cron(0 7 * * ? *)",
                "State", "ENABLED"
        ));
    }
}
