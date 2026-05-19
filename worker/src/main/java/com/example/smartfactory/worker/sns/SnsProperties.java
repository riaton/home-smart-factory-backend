package com.example.smartfactory.worker.sns;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sns")
public record SnsProperties(

        String topicArn,

        String region) {
}
