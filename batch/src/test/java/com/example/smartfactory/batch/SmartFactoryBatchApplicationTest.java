package com.example.smartfactory.batch;

import com.example.smartfactory.batch.cleanup.DataCleanupService;
import com.example.smartfactory.batch.report.ReportGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SmartFactoryBatchApplicationTest {

    @MockitoBean
    private ReportGenerationService reportGenerationService;

    @MockitoBean
    private DataCleanupService dataCleanupService;

    @Test
    @DisplayName("Spring コンテキストが正常にロードされること")
    void contextLoads() {
    }
}
