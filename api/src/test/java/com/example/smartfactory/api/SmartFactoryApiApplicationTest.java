package com.example.smartfactory.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartFactoryApiApplicationTest {

    @Test
    @DisplayName("Spring コンテキストが正常にロードされること")
    void contextLoads() {
    }
}
