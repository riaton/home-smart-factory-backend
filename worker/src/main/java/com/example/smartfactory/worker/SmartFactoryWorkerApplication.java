package com.example.smartfactory.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class SmartFactoryWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFactoryWorkerApplication.class, args);
    }
}
