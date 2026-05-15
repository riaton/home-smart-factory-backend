package com.example.smartfactory.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartFactoryBatchApplicationTest {

    @Test
    @DisplayName("Spring コンテキストが正常にロードされること")
    void contextLoads() {
    }
}
