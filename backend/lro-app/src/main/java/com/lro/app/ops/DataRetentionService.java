package com.lro.app.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data retention and pruning engine implementing Prompt 28.
 * Configures quote and temporary tick pruning while preserving trades,
 * double-entry transaction ledgers, and audit logs indefinitely.
 */
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    public record RetentionPolicy(
            Duration quoteRetentionPeriod,
            Duration candleRetentionPeriod,
            boolean preserveTradesIndefinitely,
            boolean preserveLedgerIndefinitely,
            boolean preserveAuditLogsIndefinitely
    ) {
        public static RetentionPolicy defaultPolicy() {
            return new RetentionPolicy(
                    Duration.ofDays(7),
                    Duration.ofDays(30),
                    true,
                    true,
                    true
            );
        }
    }

    public record PruneResult(
            Instant executionTime,
            int quotesPruned,
            int candlesPruned,
            int tradesPreserved,
            int ledgerEntriesPreserved,
            int auditRecordsPreserved,
            boolean dryRun
    ) {}

    public record RetainedItem(String id, String type, Instant timestamp) {}

    private final RetentionPolicy policy;
    private final List<RetainedItem> quotes = Collections.synchronizedList(new ArrayList<>());
    private final List<RetainedItem> candles = Collections.synchronizedList(new ArrayList<>());
    private final List<RetainedItem> trades = Collections.synchronizedList(new ArrayList<>());
    private final List<RetainedItem> ledgerEntries = Collections.synchronizedList(new ArrayList<>());
    private final List<RetainedItem> auditLogs = Collections.synchronizedList(new ArrayList<>());

    public DataRetentionService() {
        this(RetentionPolicy.defaultPolicy());
    }

    public DataRetentionService(RetentionPolicy policy) {
        this.policy = policy;
    }

    public void recordQuote(String id, Instant timestamp) {
        quotes.add(new RetainedItem(id, "QUOTE", timestamp));
    }

    public void recordCandle(String id, Instant timestamp) {
        candles.add(new RetainedItem(id, "CANDLE", timestamp));
    }

    public void recordTrade(String id, Instant timestamp) {
        trades.add(new RetainedItem(id, "TRADE", timestamp));
    }

    public void recordLedgerEntry(String id, Instant timestamp) {
        ledgerEntries.add(new RetainedItem(id, "LEDGER", timestamp));
    }

    public void recordAuditLog(String id, Instant timestamp) {
        auditLogs.add(new RetainedItem(id, "AUDIT", timestamp));
    }

    public PruneResult executePruning(Instant referenceTime, boolean dryRun) {
        Instant quoteCutoff = referenceTime.minus(policy.quoteRetentionPeriod());
        Instant candleCutoff = referenceTime.minus(policy.candleRetentionPeriod());

        int quotesToPrune = 0;
        int candlesToPrune = 0;

        synchronized (quotes) {
            for (RetainedItem item : quotes) {
                if (item.timestamp().isBefore(quoteCutoff)) {
                    quotesToPrune++;
                }
            }
            if (!dryRun) {
                quotes.removeIf(item -> item.timestamp().isBefore(quoteCutoff));
            }
        }

        synchronized (candles) {
            for (RetainedItem item : candles) {
                if (item.timestamp().isBefore(candleCutoff)) {
                    candlesToPrune++;
                }
            }
            if (!dryRun) {
                candles.removeIf(item -> item.timestamp().isBefore(candleCutoff));
            }
        }

        log.info("Retention job executed [dryRun={}]: pruned {} quotes, {} candles. Preserved {} trades, {} ledger entries, {} audit logs.",
                dryRun, quotesToPrune, candlesToPrune, trades.size(), ledgerEntries.size(), auditLogs.size());

        return new PruneResult(
                referenceTime,
                quotesToPrune,
                candlesToPrune,
                trades.size(),
                ledgerEntries.size(),
                auditLogs.size(),
                dryRun
        );
    }

    public int getQuoteCount() { return quotes.size(); }
    public int getCandleCount() { return candles.size(); }
    public int getTradeCount() { return trades.size(); }
    public int getLedgerCount() { return ledgerEntries.size(); }
    public int getAuditCount() { return auditLogs.size(); }
    public RetentionPolicy getPolicy() { return policy; }
}
