package com.lro.common.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Standard versioned JSON event envelope required by Prompt 12 & 18.
 * Facilitates asynchronous STOMP messaging and outbox persistence.
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        String entityId,
        T payload
) implements Serializable {

    public static <T> EventEnvelope<T> create(String eventType, String producer, String entityId, T payload) {
        return create(eventType, producer, UUID.randomUUID().toString(), entityId, payload);
    }

    public static <T> EventEnvelope<T> create(String eventType, String producer, String correlationId, String entityId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                Objects.requireNonNull(eventType, "eventType cannot be null"),
                "1.0.0",
                Instant.now(),
                Objects.requireNonNull(producer, "producer cannot be null"),
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                entityId,
                Objects.requireNonNull(payload, "payload cannot be null")
        );
    }
}
