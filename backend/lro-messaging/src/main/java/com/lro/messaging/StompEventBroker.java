package com.lro.messaging;

import com.lro.common.event.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Resilient STOMP-compatible Event Broker & Dispatcher implementing Prompt 18.
 * Provides topic-based publish/subscribe messaging across Bank, Exchange, and LRO modules.
 */
public class StompEventBroker {

    private static final Logger log = LoggerFactory.getLogger(StompEventBroker.class);

    public static final String TOPIC_MARKET_QUOTES = "/topic/market.quotes";
    public static final String TOPIC_MARKET_TRADES = "/topic/market.trades";
    public static final String TOPIC_MARKET_REGIMES = "/topic/market.regime";
    public static final String TOPIC_BANK_TRANSACTIONS = "/topic/bank.transactions";
    public static final String TOPIC_LRO_SNAPSHOTS = "/topic/lro.snapshots";
    public static final String TOPIC_LRO_ALERTS = "/topic/lro.alerts";

    private final Map<String, List<Consumer<EventEnvelope<?>>>> subscribers = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<EventEnvelope<?>> outboxQueue = new LinkedBlockingQueue<>(10000);

    private volatile boolean isRunning = true;

    public void subscribe(String destination, Consumer<EventEnvelope<?>> listener) {
        subscribe(destination, "anonymous", listener);
    }

    public void subscribe(String destination, String subscriberId, Consumer<EventEnvelope<?>> listener) {
        Objects.requireNonNull(destination, "destination cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        subscribers.computeIfAbsent(destination, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("Subscribed subscriber '{}' to {}", subscriberId, destination);
    }

    public boolean publish(String destination, EventEnvelope<?> event) {
        if (!isRunning || event == null) return false;

        // Idempotency check: discard if already processed
        if (!processedEventIds.add(event.eventId())) {
            log.warn("Duplicate event discarded: {}", event.eventId());
            return false;
        }

        // Buffer into outbox
        outboxQueue.offer(event);

        // Immediate dispatch to subscribers
        List<Consumer<EventEnvelope<?>>> listeners = subscribers.get(destination);
        if (listeners != null && !listeners.isEmpty()) {
            for (Consumer<EventEnvelope<?>> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception ex) {
                    log.error("Error dispatching event {} to listener on {}: {}", event.eventId(), destination, ex.getMessage(), ex);
                }
            }
        }
        return true;
    }

    public int getSubscriberCount(String destination) {
        List<Consumer<EventEnvelope<?>>> list = subscribers.get(destination);
        return list != null ? list.size() : 0;
    }

    public int getProcessedEventCount() {
        return processedEventIds.size();
    }

    public boolean isHealthy() {
        return isRunning;
    }

    public void shutdown() {
        this.isRunning = false;
        subscribers.clear();
    }
}
