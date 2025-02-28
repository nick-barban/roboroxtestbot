package com.nb.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.jms.ConnectionFactory;
import software.amazon.awssdk.services.sqs.SqsClient;

@Factory
public class JmsConfig {
    
    @Bean
    ConnectionFactory connectionFactory(SqsClient sqsClient) {
        return new SQSConnectionFactory(
            new ProviderConfiguration(),
            sqsClient
        );
    }
} 