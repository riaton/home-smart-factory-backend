package com.example.smartfactory.worker.sqs;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.sqs.polling-enabled", matchIfMissing = true)
public class SqsPoller implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SqsPoller.class);

    private static final int WAIT_TIME_SECONDS = 20;

    private final SqsClient sqsClient;

    private final SqsProperties sqsProperties;

    private final IotMessageProcessor processor;

    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("SQS polling started: {}", sqsProperties.queueUrl());
        while (running) {
            try {
                ReceiveMessageResponse response = sqsClient.receiveMessage(
                        ReceiveMessageRequest.builder()
                                .queueUrl(sqsProperties.queueUrl())
                                .maxNumberOfMessages(1)
                                .waitTimeSeconds(WAIT_TIME_SECONDS)
                                .build());

                List<Message> messages = response.messages();
                for (Message message : messages) {
                    processor.process(message);
                }
            } catch (Exception e) {
                LOG.error("SQS receive error: {}", e.getMessage());
            }
        }
        LOG.info("SQS polling stopped.");
    }

    @PreDestroy
    public void stop() {
        running = false;
    }
}
