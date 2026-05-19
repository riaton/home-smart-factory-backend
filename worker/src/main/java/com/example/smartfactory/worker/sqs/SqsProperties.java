package com.example.smartfactory.worker.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(

        String queueUrl,

        String region) {
}
