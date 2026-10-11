package com.lro.app;

import com.lro.app.ingestion.HistoricalDataIngestionService;
import com.lro.app.ingestion.HistoricalDataIngestionService.DatasetMetadata;
import com.lro.app.ingestion.HistoricalDataIngestionService.IngestionReport;
import com.lro.app.ingestion.HistoricalDataIngestionService.OhlcvRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PROMPT 30: Historical Market Data Ingestion and Validation Controls")
class HistoricalIngestionTest {

    private HistoricalDataIngestionService ingestionService;
    private DatasetMetadata validMetadata;

    @BeforeEach
    void setUp() {
        ingestionService = new HistoricalDataIngestionService();
        validMetadata = new DatasetMetadata(
                "BATCH-HIST-2026-001",
                "NSE Official EOD Market Report (Educational)",
                "NSE Nifty 50 Historical EOD",
                Instant.now(),
                "https://www.nseindia.com/reports/historical-eod",
                "Non-commercial academic research and simulation purposes only",
                "Split and dividend adjusted back-series",
                "Asia/Kolkata",
                "INR"
        );
    }

    @Test
    @DisplayName("Ingestion 1: Valid chronological OHLCV series ingests successfully")
    void testValidOhlcvIngestion() {
        List<OhlcvRow> rows = List.of(
                new OhlcvRow(LocalDate.of(2026, 1, 1), "RELIANCE", BigDecimal.valueOf(2800.0), BigDecimal.valueOf(2850.0), BigDecimal.valueOf(2790.0), BigDecimal.valueOf(2840.0), 1500000),
                new OhlcvRow(LocalDate.of(2026, 1, 2), "RELIANCE", BigDecimal.valueOf(2840.0), BigDecimal.valueOf(2870.0), BigDecimal.valueOf(2830.0), BigDecimal.valueOf(2865.0), 1800000)
        );

        IngestionReport report = ingestionService.ingestOhlcvBatch(validMetadata, rows, false);

        assertTrue(report.success());
        assertEquals(2, report.validRows());
        assertTrue(report.validationErrors().isEmpty());
        assertTrue(ingestionService.isBatchIngested("BATCH-HIST-2026-001"));
        assertEquals(2, ingestionService.getSeries("BATCH-HIST-2026-001").size());
    }

    @Test
    @DisplayName("Ingestion 2: Duplicate batch ingestion is rejected idempotently")
    void testDuplicateBatchRejection() {
        List<OhlcvRow> rows = List.of(
                new OhlcvRow(LocalDate.of(2026, 1, 1), "TCS", BigDecimal.valueOf(3800.0), BigDecimal.valueOf(3850.0), BigDecimal.valueOf(3790.0), BigDecimal.valueOf(3840.0), 500000)
        );

        // First ingest
        IngestionReport r1 = ingestionService.ingestOhlcvBatch(validMetadata, rows, false);
        assertTrue(r1.success());

        // Duplicate ingest with same batch ID
        IngestionReport r2 = ingestionService.ingestOhlcvBatch(validMetadata, rows, false);
        assertFalse(r2.success());
        assertTrue(r2.validationErrors().get(0).contains("Duplicate batch ID"));
    }

    @Test
    @DisplayName("Ingestion 3: Chronological ordering and high/low envelope violations are caught")
    void testMalformedOhlcvRejection() {
        DatasetMetadata metadata = new DatasetMetadata(
                "BATCH-FAIL-01", "Provider", "Set", Instant.now(), "url", "Licence", "Adj", "Asia/Kolkata", "INR"
        );

        List<OhlcvRow> malformedRows = List.of(
                // Row 1: Valid
                new OhlcvRow(LocalDate.of(2026, 1, 5), "INFY", BigDecimal.valueOf(1500.0), BigDecimal.valueOf(1550.0), BigDecimal.valueOf(1490.0), BigDecimal.valueOf(1540.0), 200000),
                // Row 2: Date out of order (Jan 2 after Jan 5)
                new OhlcvRow(LocalDate.of(2026, 1, 2), "INFY", BigDecimal.valueOf(1540.0), BigDecimal.valueOf(1560.0), BigDecimal.valueOf(1530.0), BigDecimal.valueOf(1555.0), 200000),
                // Row 3: High is lower than Low (Envelope violation)
                new OhlcvRow(LocalDate.of(2026, 1, 6), "INFY", BigDecimal.valueOf(1555.0), BigDecimal.valueOf(1400.0), BigDecimal.valueOf(1600.0), BigDecimal.valueOf(1550.0), 200000)
        );

        IngestionReport report = ingestionService.ingestOhlcvBatch(metadata, malformedRows, false);

        assertFalse(report.success());
        assertTrue(report.validationErrors().size() >= 2);
        assertFalse(ingestionService.isBatchIngested("BATCH-FAIL-01"));
    }

    @Test
    @DisplayName("Ingestion 4: Dry-run mode validates data without persisting to storage")
    void testDryRunValidationMode() {
        DatasetMetadata metadata = new DatasetMetadata(
                "BATCH-DRY-RUN", "Provider", "Set", Instant.now(), "url", "Licence", "Adj", "Asia/Kolkata", "INR"
        );

        List<OhlcvRow> rows = List.of(
                new OhlcvRow(LocalDate.of(2026, 1, 1), "HDFCBANK", BigDecimal.valueOf(1650.0), BigDecimal.valueOf(1680.0), BigDecimal.valueOf(1640.0), BigDecimal.valueOf(1670.0), 3000000)
        );

        IngestionReport report = ingestionService.ingestOhlcvBatch(metadata, rows, true);

        assertTrue(report.success());
        assertTrue(report.dryRun());
        assertFalse(ingestionService.isBatchIngested("BATCH-DRY-RUN"), "Dry run must not mark batch as ingested");
    }

    @Test
    @DisplayName("Ingestion 5: Missing licence metadata is rejected with IllegalArgumentException")
    void testMissingLicenceMetadata() {
        assertThrows(IllegalArgumentException.class, () ->
                new DatasetMetadata(
                        "BATCH-NO-LIC", "Provider", "Set", Instant.now(), "url", "", "Adj", "Asia/Kolkata", "INR"
                ).validate()
        );
    }
}
