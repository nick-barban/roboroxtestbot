package com.nb;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@MicronautTest(/*packages = {"service", "config"}*/)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbstractTest implements TestPropertyProvider {
    protected static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");
    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(LocalStackContainer.Service.SQS);

    @Override
    public @NonNull Map<String, String> getProperties() {
        if (!LOCALSTACK.isRunning()) {
            LOCALSTACK.start();
        }
        return Map.of("aws.access-key-id", LOCALSTACK.getAccessKey(),
                "aws.secret-key", LOCALSTACK.getSecretKey(),
                "aws.region", LOCALSTACK.getRegion(),
                "aws.services.sqs.endpoint-override", LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
    }
}
