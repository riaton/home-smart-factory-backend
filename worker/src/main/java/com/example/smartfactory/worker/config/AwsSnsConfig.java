package com.example.smartfactory.worker.config;

import com.example.smartfactory.worker.sns.SnsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsSnsConfig {

    @Bean
    public SnsClient snsClient(SnsProperties properties) {
        return SnsClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
