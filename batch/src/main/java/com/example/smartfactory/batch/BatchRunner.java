package com.example.smartfactory.batch;

import com.example.smartfactory.batch.cleanup.DataCleanupService;
import com.example.smartfactory.batch.report.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRunner.class);

    private final ReportGenerationService reportGenerationService;

    private final DataCleanupService dataCleanupService;

    @Override
    public void run(String... args) {
        LOG.info("Starting daily report batch");
        reportGenerationService.generateDailyReports();
        dataCleanupService.cleanupOldData();
        LOG.info("Daily report batch completed");
    }
}
