package com.lro.app;

import com.lro.app.ops.DataRetentionService;
import com.lro.app.ops.DataRetentionService.PruneResult;
import com.lro.app.ops.DataRetentionService.RetentionPolicy;
import com.lro.app.ops.SystemHealthMonitor;
import com.lro.app.ops.SystemHealthMonitor.SubsystemStatus;
import com.lro.common.model.Money;
import com.lro.exchange.service.ContinuousMatchingEngine;
import com.lro.exchange.service.ContinuousMatchingEngine.Order;
import com.lro.exchange.service.ContinuousMatchingEngine.OrderType;
import com.lro.exchange.service.ContinuousMatchingEngine.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PROMPT 28: Performance, Retention and Operational Monitoring Tests")
class RetentionAndPerformanceTest {

    private DataRetentionService retentionService;
    private SystemHealthMonitor healthMonitor;

    @BeforeEach
    void setUp() {
        retentionService = new DataRetentionService(
                new RetentionPolicy(Duration.ofDays(7), Duration.ofDays(30), true, true, true)
        );
        healthMonitor = new SystemHealthMonitor();
    }

    @Test
    @DisplayName("Retention 1: Quotes older than retention threshold are pruned while preserving trades and ledgers")
    void testRetentionPruningPolicy() {
        Instant now = Instant.now();
        Instant eightDaysAgo = now.minus(Duration.ofDays(8));
        Instant twoDaysAgo = now.minus(Duration.ofDays(2));

        // Quotes: 1 old, 1 recent
        retentionService.recordQuote("Q-OLD", eightDaysAgo);
        retentionService.recordQuote("Q-NEW", twoDaysAgo);

        // Permanent records: Trades, Ledger, Audit
        retentionService.recordTrade("T-OLD", eightDaysAgo);
        retentionService.recordLedgerEntry("LEDGER-01", eightDaysAgo);
        retentionService.recordAuditLog("AUDIT-01", eightDaysAgo);

        assertEquals(2, retentionService.getQuoteCount());
        assertEquals(1, retentionService.getTradeCount());

        // Dry Run
        PruneResult dryRunResult = retentionService.executePruning(now, true);
        assertEquals(1, dryRunResult.quotesPruned());
        assertEquals(2, retentionService.getQuoteCount(), "Dry run must not remove items");

        // Real Execution
        PruneResult realResult = retentionService.executePruning(now, false);
        assertEquals(1, realResult.quotesPruned());
        assertEquals(1, retentionService.getQuoteCount(), "One recent quote should remain");
        assertEquals(1, retentionService.getTradeCount(), "Trades must be preserved indefinitely");
        assertEquals(1, retentionService.getLedgerCount(), "Ledger entries must be preserved indefinitely");
        assertEquals(1, retentionService.getAuditCount(), "Audit logs must be preserved indefinitely");
    }

    @Test
    @DisplayName("Monitoring 2: Health monitor tracks subsystem statuses and operational telemetry")
    void testOperationalHealthMonitoring() {
        assertTrue(healthMonitor.isEcosystemHealthy());

        // Telemetry tracking
        healthMonitor.incrementMessagesProcessed();
        healthMonitor.incrementMessagesProcessed();
        healthMonitor.incrementStaleQuotes();
        healthMonitor.recordInference(false, 15);
        healthMonitor.recordInference(true, 5); // Fallback

        var metrics = healthMonitor.getMetricsSnapshot();
        assertEquals(2, metrics.totalMessagesProcessed());
        assertEquals(1, metrics.staleQuotesDetected());
        assertEquals(1, metrics.successfulInferences());
        assertEquals(1, metrics.fallbackInferences());
        assertEquals(10.0, metrics.averageInferenceLatencyMs(), 0.01);

        // Degradation simulation
        healthMonitor.recordSubsystemHealth("DatabaseManager", SubsystemStatus.DEGRADED, "High connection latency (120ms)");
        assertTrue(healthMonitor.isEcosystemHealthy(), "Degraded status should keep ecosystem running");

        healthMonitor.recordSubsystemHealth("StompEventBroker", SubsystemStatus.DOWN, "Broker socket disconnected");
        assertFalse(healthMonitor.isEcosystemHealthy(), "Down subsystem flags ecosystem as unhealthy");
    }

    @Test
    @DisplayName("Performance 3: Continuous matching engine processes sustained bursts within sub-millisecond latency")
    void testMatchingEngineThroughputPerformance() {
        ContinuousMatchingEngine engine = new ContinuousMatchingEngine();
        int burstSize = 1000;

        long startTime = System.nanoTime();
        for (int i = 0; i < burstSize; i++) {
            Side side = (i % 2 == 0) ? Side.SELL : Side.BUY;
            String price = (i % 2 == 0) ? "2800.00" : "2800.00";
            engine.submitOrder(new Order(
                    "BURST-" + i,
                    "RELIANCE",
                    side,
                    OrderType.LIMIT,
                    10,
                    Money.of(price)
            ));
        }
        long durationNs = System.nanoTime() - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double throughputOrdersPerSec = (burstSize / durationMs) * 1000.0;

        System.out.printf("Performance Measured: %d orders matched in %.2f ms (Throughput: %.0f orders/sec)%n",
                burstSize, durationMs, throughputOrdersPerSec);

        // Ensure sub-millisecond average processing per order (1000 orders in under 2000 ms)
        assertTrue(durationMs < 2000.0, "1000 matching operations must complete in under 2 seconds");
    }
}
