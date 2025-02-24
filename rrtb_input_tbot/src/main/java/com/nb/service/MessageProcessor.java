package com.nb.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.micronaut.context.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Singleton
public class MessageProcessor {

//    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);
//    private final AmazonSQS sqsClient;
//    private final String myQueueURL;

    /*public MessageProcessor(@Value("${app.queue.input.name}") String queueName, AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
        createQueueRequest.addAttributesEntry(QueueAttributeName.FifoQueue.name(), "true");
        createQueueRequest.addAttributesEntry(QueueAttributeName.ContentBasedDeduplication.name(), "true");
        this.myQueueURL = this.sqsClient.createQueue(createQueueRequest).getQueueUrl();
    }*/

    /*public String processMessage(String msg) {
        final SendMessageResult result = sendMessage(msg);
        final String messageId = result.getMessageId();
        LOG.info("Message sent. MessageId: {}", messageId);
        return messageId;
    }*/

    /*private SendMessageResult sendMessage(String msg) {
        return sqsClient.sendMessage(new SendMessageRequest()
                .withQueueUrl(this.myQueueURL)
                .withMessageBody(msg));
    }*/
}
