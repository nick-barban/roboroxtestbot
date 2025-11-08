package com.nb;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.objectstorage.aws.AwsS3Configuration;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.IOException;

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
public class AbstractTest implements TestPropertyProvider {

    @Inject
    BeanContext ctx;

    @Inject
    ResourceLoader resourceLoader;

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

    @AfterAll
    static void tearDown() {
        LOCAL_STACK_CONTAINER.close();
    }

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

    @Test
    public void shouldParseBodyFromPost() {
        String resourcePath = "classpath:posts/RRFenixThursday.post";
        Optional<InputStream> inputStreamOptional = resourceLoader.getResourceAsStream(resourcePath);

        Assertions.assertThat(inputStreamOptional).isPresent();

        try (InputStream inputStream = inputStreamOptional.get();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String fileContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            System.out.println("File Content Loaded by ResourceLoader:");
            System.out.println(fileContent); 
            Assertions.assertThat(fileContent).isNotNull();
            Assertions.assertThat(fileContent).contains("#telegramGroup: https://t.me/RoboRoxFenix");

        } catch (IOException e) {
            Assertions.fail("Failed to read resource: " + resourcePath, e);
        }
    }

    @Test
    public void shouldExtractBodyFromPostFile() {
        String resourcePath = "classpath:posts/RRFenixThursday.post";
        Optional<InputStream> inputStreamOptional = resourceLoader.getResourceAsStream(resourcePath);

        Assertions.assertThat(inputStreamOptional).as("Resource stream for " + resourcePath).isPresent();

        try (InputStream inputStream = inputStreamOptional.get();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String fileContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            String[] parts = fileContent.split("#body:\\R", 2); // \\R matches any Unicode newline sequence
            String body = null;
            if (parts.length > 1) {
                body = parts[1];
            }

            Assertions.assertThat(body).as("Extracted body from post file").isNotNull();
            Assertions.assertThat(body.trim()).as("Trimmed extracted body").isNotEmpty();
            Assertions.assertThat(body.trim()).startsWith("Доброго дня");

        } catch (IOException e) {
            Assertions.fail("Failed to read or parse resource: " + resourcePath, e);
        }
    }
}
