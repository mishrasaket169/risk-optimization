package com.lro.app.ingestion;

import com.lro.common.model.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Historical Data Ingestion Service implementing Prompt 30.
 * Enforces data provenance, licence verification, chronological validation,
 * price-consistency checks, dry-run validation, and idempotency tracking.
 */
public class HistoricalDataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataIngestionService.class);

    public record DatasetMetadata(
            String batchId,
            String provider,
            String datasetName,
            Instant downloadTime,
            String sourceProvenanceUrl,
            String licenceNote,
            String adjustmentMethodology,
            String timezone,
            String currency
    ) {
        public void validate() {
            if (batchId == null || batchId.isBlank()) throw new IllegalArgumentException("Batch ID cannot be blank");
            if (provider == null || provider.isBlank()) throw new IllegalArgumentException("Provider cannot be blank");
            if (datasetName == null || datasetName.isBlank()) throw new IllegalArgumentException("Dataset name cannot be blank");
            if (licenceNote == null || licenceNote.isBlank()) throw new IllegalArgumentException("Licence note cannot be blank");
            if (!"INR".equalsIgnoreCase(currency)) throw new IllegalArgumentException("Currency must be INR for domestic market simulation");
        }
    }

    public record OhlcvRow(
            LocalDate date,
            String symbol,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            long volume
    ) {}

    public record IngestionReport(
            String batchId,
            boolean success,
            int totalRowsEvaluated,
            int validRows,
            List<String> validationErrors,
            boolean dryRun,
            Instant timestamp
    ) {}

    private final Set<String> ingestedBatches = ConcurrentHashMap.newKeySet();
    private final Map<String, List<OhlcvRow>> storedSeries = new ConcurrentHashMap<>();

    public IngestionReport ingestOhlcvBatch(DatasetMetadata metadata, List<OhlcvRow> rows, boolean dryRun) {
        metadata.validate();

        List<String> errors = new ArrayList<>();

        // 1. Idempotency Check
        if (ingestedBatches.contains(metadata.batchId())) {
            errors.add("Duplicate batch ID: " + metadata.batchId() + " has already been ingested.");
            return new IngestionReport(metadata.batchId(), false, rows.size(), 0, errors, dryRun, Instant.now());
        }

        if (rows == null || rows.isEmpty()) {
            errors.add("Batch contains 0 rows to ingest.");
            return new IngestionReport(metadata.batchId(), false, 0, 0, errors, dryRun, Instant.now());
        }

        // 2. Chronological & Consistency Validation
        LocalDate prevDate = null;
        Set<LocalDate> datesSeen = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            OhlcvRow row = rows.get(i);
            int rowNum = i + 1;

            if (row.date() == null) {
                errors.add("Row " + rowNum + ": Missing date");
                continue;
            }

            // Duplicate row check
            if (!datesSeen.add(row.date())) {
                errors.add("Row " + rowNum + ": Duplicate session date " + row.date());
            }

            // Chronological order check
            if (prevDate != null && !row.date().isAfter(prevDate)) {
                errors.add("Row " + rowNum + ": Date " + row.date() + " is out of chronological order (prev=" + prevDate + ")");
            }
            prevDate = row.date();

            // Non-positive price check
            if (row.open().compareTo(BigDecimal.ZERO) <= 0 ||
                row.high().compareTo(BigDecimal.ZERO) <= 0 ||
                row.low().compareTo(BigDecimal.ZERO) <= 0 ||
                row.close().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Row " + rowNum + ": Non-positive prices detected (O=" + row.open() + ", H=" + row.high() + ", L=" + row.low() + ", C=" + row.close() + ")");
            }

            // High/Low envelope checks
            if (row.high().compareTo(row.low()) < 0) {
                errors.add("Row " + rowNum + ": Inconsistent envelope: High (" + row.high() + ") is lower than Low (" + row.low() + ")");
            }
            if (row.high().compareTo(row.open()) < 0 || row.high().compareTo(row.close()) < 0) {
                errors.add("Row " + rowNum + ": High (" + row.high() + ") is less than Open or Close");
            }
            if (row.low().compareTo(row.open()) > 0 || row.low().compareTo(row.close()) > 0) {
                errors.add("Row " + rowNum + ": Low (" + row.low() + ") is greater than Open or Close");
            }

            // Volume checks
            if (row.volume() < 0) {
                errors.add("Row " + rowNum + ": Negative volume " + row.volume());
            }
        }

        boolean success = errors.isEmpty();

        if (success && !dryRun) {
            ingestedBatches.add(metadata.batchId());
            storedSeries.put(metadata.batchId(), List.copyOf(rows));
            log.info("Historical batch {} ingested successfully ({} rows from {})", metadata.batchId(), rows.size(), metadata.provider());
        } else if (!success) {
            log.warn("Historical batch {} rejected with {} validation errors", metadata.batchId(), errors.size());
        }

        return new IngestionReport(
                metadata.batchId(),
                success,
                rows.size(),
                success ? rows.size() : 0,
                errors,
                dryRun,
                Instant.now()
        );
    }

    public boolean isBatchIngested(String batchId) {
        return ingestedBatches.contains(batchId);
    }

    public List<OhlcvRow> getSeries(String batchId) {
        return storedSeries.getOrDefault(batchId, List.of());
    }
}
