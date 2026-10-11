package com.lro.app;

import com.lro.analytics.PortfolioValuationEngine;
import com.lro.analytics.PortfolioValuationEngine.PortfolioLot;
import com.lro.common.event.EventEnvelope;
import com.lro.common.model.Money;
import com.lro.exchange.service.ContinuousMatchingEngine;
import com.lro.exchange.service.ContinuousMatchingEngine.Order;
import com.lro.exchange.service.ContinuousMatchingEngine.OrderType;
import com.lro.exchange.service.ContinuousMatchingEngine.Side;
import com.lro.exchange.service.ExchangeInstrumentCatalogue;
import com.lro.exchange.service.ExchangeInstrumentCatalogue.Instrument;
import com.lro.messaging.StompEventBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PROMPT 29: Service Contracts and Cross-Application Consistency Tests")
class ServiceContractTest {

    private ExchangeInstrumentCatalogue catalogue;
    private ContinuousMatchingEngine matchingEngine;
    private PortfolioValuationEngine valuationEngine;
    private StompEventBroker broker;

    @BeforeEach
    void setUp() {
        catalogue = new ExchangeInstrumentCatalogue();
        matchingEngine = new ContinuousMatchingEngine();
        broker = new StompEventBroker();
        valuationEngine = new PortfolioValuationEngine(broker);
    }

    @Test
    @DisplayName("Contract 1: Instrument catalogue identifiers match across Bank Holdings and Exchange Book")
    void testInstrumentConsistencyAcrossSubsystems() {
        // Equities
        Optional<Instrument> reliance = catalogue.findInstrument("RELIANCE");
        assertTrue(reliance.isPresent());
        assertEquals("Equity", reliance.get().category());
        assertEquals("INR", reliance.get().currency());

        // G-Secs
        Optional<Instrument> gsec = catalogue.findInstrument("GS-7.18-2033");
        assertTrue(gsec.isPresent());
        assertEquals("G-Sec", gsec.get().category());
        assertEquals("GOVERNMENT_OF_INDIA", gsec.get().issuer());

        // Bank portfolio holds the exact same instrument ID
        valuationEngine.addLot(new PortfolioLot(
                "LOT-CONTRACT-01", "BANK-01-LRG", reliance.get().symbol(),
                reliance.get().category(), 1000L, Money.of("2800.00"), Money.of("2800.00"), false, Instant.now()
        ));

        assertEquals(Money.of("2800000.00"), valuationEngine.getTotalPortfolioMarketValue("BANK-01-LRG"));
    }

    @Test
    @DisplayName("Contract 2: Financial units and rounding rules strictly adhere to Banker's Rounding (HALF_EVEN)")
    void testFinancialUnitsAndPrecision() {
        Money m1 = Money.of(new BigDecimal("100.125"));
        Money m2 = Money.of(new BigDecimal("100.135"));

        // Banker's rounding: 100.125 -> 100.12 (even), 100.135 -> 100.14 (even)
        assertEquals(new BigDecimal("100.12"), m1.amount());
        assertEquals(new BigDecimal("100.14"), m2.amount());
        assertEquals("INR", m1.currency());
    }

    @Test
    @DisplayName("Contract 3: Event schema envelopes follow standard 1.0.0 contract and correlation tracking")
    void testEventEnvelopeContract() {
        EventEnvelope<String> envelope = EventEnvelope.create(
                "TRADE_EXECUTED",
                "ExchangeSimulator",
                "CORR-XYZ-999",
                "TRADE-001",
                "{\"symbol\":\"RELIANCE\",\"qty\":100}"
        );

        assertNotNull(envelope.eventId());
        assertEquals("1.0.0", envelope.schemaVersion());
        assertEquals("TRADE_EXECUTED", envelope.eventType());
        assertEquals("ExchangeSimulator", envelope.producer());
        assertEquals("CORR-XYZ-999", envelope.correlationId());
        assertEquals("TRADE-001", envelope.entityId());
        assertNotNull(envelope.occurredAt());
    }

    @Test
    @DisplayName("Contract 4: Broker idempotency and message deduplication by eventId")
    void testBrokerDeduplicationContract() {
        AtomicInteger deliveryCount = new AtomicInteger(0);
        broker.subscribe("/topic/market/trades", "contract-sub", envelope -> {
            deliveryCount.incrementAndGet();
        });

        EventEnvelope<String> event = EventEnvelope.create(
                "TRADE_EXECUTED",
                "ExchangeSimulator",
                "CORR-01",
                "TRADE-01",
                "Payload"
        );

        // Publish once
        broker.publish("/topic/market/trades", event);
        assertEquals(1, deliveryCount.get());

        // Publish duplicate with identical eventId
        broker.publish("/topic/market/trades", event);
        assertEquals(1, deliveryCount.get(), "Duplicate eventId must be silently discarded by broker idempotency filter");
    }

    @Test
    @DisplayName("Contract 5: Unknown instrument is safely rejected without cascading failure")
    void testUnknownInstrumentSafety() {
        Optional<Instrument> unknown = catalogue.findInstrument("UNKNOWN_TICKER");
        assertTrue(unknown.isEmpty());

        // Valuation engine gracefully handles valuation for unknown instrument
        Money val = valuationEngine.getTotalPortfolioMarketValue("NON_EXISTENT_BANK");
        assertEquals(Money.ZERO, val);
    }
}
