package com.nb;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.objectstorage.aws.AwsS3Configuration;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.IAM;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KMS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.STS;

@MicronautTest
@TestInstance(PER_CLASS)
@Testcontainers
abstract class AbstractTest implements TestPropertyProvider {

    @Inject
    BeanContext ctx;

    public static final String GIVEN_TEST_INPUT_FIFO_QUEUE_NAME = "test_input.fifo";
    public static final String GIVEN_TEST_OUTPUT_FIFO_QUEUE_NAME = "test_output.fifo";

    private static final LocalStackContainer.EnabledService EVENT_BRIDGE = new LocalStackContainer.EnabledService() {
        @Override
        public String getName() {
            return "scheduler";
        }

        @Override
        public int getPort() {
            return 8030;
        }
    };
    private static final LocalStackContainer.EnabledService LOGS = new LocalStackContainer.EnabledService() {
        @Override
        public int getPort() {
            return 8031;
        }

        @Override
        public String getName() {
            return "logs";
        }
    };

    @Container
    protected static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest"))
            .withServices(SQS, LAMBDA, CLOUDWATCH, KMS, STS, IAM, EVENT_BRIDGE, LOGS)
            .withCopyFileToContainer(MountableFile.forHostPath("target/rrtb_daily_post_lambda-0.1.jar"), "/opt/code/localstack/lambda.jar");

    protected static void assertResult(org.testcontainers.containers.Container.ExecResult execResult) {
        Assertions.assertThat(execResult.getExitCode()).isEqualTo(0);
        Assertions.assertThat(execResult.getStderr()).isEmpty();
    }

    protected InputStream getScheduledEventJson() {
        return SchedulerHandlerTest.class.getResourceAsStream("/mockScheduledEvent.json");
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
        if (!LOCAL_STACK_CONTAINER.isRunning()) {
            LOCAL_STACK_CONTAINER.start();
        }
        return Map.of(
                "app.queue.input.name", GIVEN_TEST_INPUT_FIFO_QUEUE_NAME,
                "app.queue.output.name", GIVEN_TEST_OUTPUT_FIFO_QUEUE_NAME,
                "aws.access-key-id", LOCAL_STACK_CONTAINER.getAccessKey(),
                "aws.secret-key", LOCAL_STACK_CONTAINER.getSecretKey(),
                "aws.region", LOCAL_STACK_CONTAINER.getRegion(),
                "aws.services.sqs.endpoint-override", LOCAL_STACK_CONTAINER.getEndpointOverride(LocalStackContainer.Service.SQS).toString()
        );
    }

    @Test
    public void shouldCheckBeansExist() {
        Assertions.assertThat(ctx.containsBean(AwsS3Operations.class)).isTrue();
    }

    @Test
    void shouldCreateMountableFileforHostFileOnWindows() {
        final MountableFile given = MountableFile.forHostPath("target/rrtb_daily_post_lambda-0.1.jar");

        final String mountablePath = given.getResolvedPath();

        final File actual = new File(mountablePath);
        Assertions.assertThat(actual).exists();
    }

    @Test
    public void shouldCreateAwsS3Configuration() {
        final AwsS3Configuration configuration = new AwsS3Configuration("posts");
        configuration.setBucket("invalid_bucket");
    }
}
