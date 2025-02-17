package com.nb.config;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;

@Singleton
class SqsClientBuilderListener implements BeanCreatedEventListener<SqsClientBuilder> {

    private final SqsConfig sqsConfig;

    SqsClientBuilderListener(SqsConfig sqsConfig) {
        this.sqsConfig = sqsConfig;
    }

    @Override
    public SqsClientBuilder onCreated(@NonNull BeanCreatedEvent<SqsClientBuilder> event) {
        SqsClientBuilder builder = event.getBean();
        try {
            final String endpointOverride = sqsConfig.getSqs().getEndpointOverride();
            final String accessKeyId = sqsConfig.getAccessKeyId();
            final String secretKey = sqsConfig.getSecretKey();
            final String region = sqsConfig.getRegion();

            return builder
                    .endpointOverride(new URI(endpointOverride))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKeyId, secretKey)
                            )
                    )
                    .region(Region.of(region));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}