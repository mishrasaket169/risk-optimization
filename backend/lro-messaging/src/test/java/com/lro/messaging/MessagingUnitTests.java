package com.lro.messaging;

import com.lro.common.event.EventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MessagingUnitTests {

    @Test
    @DisplayName("Verify Prompt 18: STOMP event publication and topic dispatch")
    void testTopicPublicationAndDispatch() {
        StompEventBroker broker = new StompEventBroker();
        assertTrue(broker.isHealthy());

        AtomicReference<String> receivedPayload = new AtomicReference<>();
        broker.subscribe(StompEventBroker.TOPIC_MARKET_QUOTES, event -> {
            receivedPayload.set((String) event.payload());
        });

        EventEnvelope<String> event = EventEnvelope.create("QUOTE_UPDATE", "SIMULATOR", "RELIANCE", "{\"price\": 2840.50}");
        boolean published = broker.publish(StompEventBroker.TOPIC_MARKET_QUOTES, event);

        assertTrue(published);
        assertEquals("{\"price\": 2840.50}", receivedPayload.get());
    }

    @Test
    @DisplayName("Verify Prompt 18: Duplicate event delivery is filtered by idempotency check")
    void testIdempotencyDeduplication() {
        StompEventBroker broker = new StompEventBroker();
        AtomicInteger count = new AtomicInteger(0);

        broker.subscribe(StompEventBroker.TOPIC_BANK_TRANSACTIONS, event -> {
            count.incrementAndGet();
        });

        EventEnvelope<String> event = EventEnvelope.create("TXN_POSTED", "BANK_ENGINE", "TXN-101", "{\"debit\": 50000}");

        boolean first = broker.publish(StompEventBroker.TOPIC_BANK_TRANSACTIONS, event);
        boolean duplicate = broker.publish(StompEventBroker.TOPIC_BANK_TRANSACTIONS, event);

        assertTrue(first, "First delivery must succeed");
        assertFalse(duplicate, "Duplicate delivery must be rejected");
        assertEquals(1, count.get(), "Listener must only be invoked once");
    }
}
