package com.example.smartfactory.api.report.dto;

import java.time.LocalDate;

public record ReportDownloadResult(byte[] pdf, LocalDate reportDate) {}
