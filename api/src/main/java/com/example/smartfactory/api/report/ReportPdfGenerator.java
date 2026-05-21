package com.example.smartfactory.api.report;

import com.example.smartfactory.api.report.dto.AnomalySummaryItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ReportPdfGenerator {

    public byte[] generate(DailyReport report, List<AnomalySummaryItem> anomalySummary) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float y = 780;

                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(50, y);
                cs.showText("Daily Report: " + report.getReportDate());
                cs.endText();

                y -= 40;
                y = writeField(cs, regular, bold, y, "Total Power (kWh)", format(report.getTotalPowerKwh()));
                y = writeField(cs, regular, bold, y, "Avg Temperature (C)", format(report.getAvgTemperature()));
                y = writeField(cs, regular, bold, y, "Avg Humidity (%)", format(report.getAvgHumidity()));
                y = writeField(cs, regular, bold, y, "Total Motion Minutes", format(report.getTotalMotionMinutes()));
                y = writeField(cs, regular, bold, y, "Anomaly Count", String.valueOf(report.getAnomalyCount()));

                if (anomalySummary != null && !anomalySummary.isEmpty()) {
                    y -= 20;
                    cs.beginText();
                    cs.setFont(bold, 13);
                    cs.newLineAtOffset(50, y);
                    cs.showText("Anomaly Summary:");
                    cs.endText();
                    y -= 20;

                    for (AnomalySummaryItem item : anomalySummary) {
                        String line = String.format("  %s / %s  count=%d  max=%.2f",
                                item.deviceId(), item.metricType(),
                                item.count(),
                                item.maxValue() != null ? item.maxValue() : BigDecimal.ZERO);
                        cs.beginText();
                        cs.setFont(regular, 11);
                        cs.newLineAtOffset(50, y);
                        cs.showText(line);
                        cs.endText();
                        y -= 18;
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("PDF generation failed", e);
        }
    }

    private float writeField(PDPageContentStream cs,
            PDType1Font regular, PDType1Font bold,
            float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(bold, 11);
        cs.newLineAtOffset(50, y);
        cs.showText(label + ": ");
        cs.endText();

        cs.beginText();
        cs.setFont(regular, 11);
        cs.newLineAtOffset(230, y);
        cs.showText(value);
        cs.endText();

        return y - 22;
    }

    private String format(Object value) {
        return value != null ? value.toString() : "-";
    }
}
