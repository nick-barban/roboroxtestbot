package com.nb.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import jakarta.inject.Singleton;

//@Singleton
public class AmazonSqsConfig /*extends AmazonSQSClient*/ {
    /*private static final AmazonSQS CLIENT = AmazonSQSClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build();

    @Override
    public SendMessageResult sendMessage(SendMessageRequest request) {
        return CLIENT.sendMessage(request);
    }*/
}
