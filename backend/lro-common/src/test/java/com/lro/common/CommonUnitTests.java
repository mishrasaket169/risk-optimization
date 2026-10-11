package com.lro.common;

import com.lro.common.event.EventEnvelope;
import com.lro.common.model.Money;
import com.lro.common.security.SecurityContext.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CommonUnitTests {

    @Test
    void testMoneyArithmeticPrecision() {
        Money m1 = Money.of("100.50");
        Money m2 = Money.of("49.50");

        Money sum = m1.add(m2);
        assertEquals(Money.of("150.00"), sum);

        Money product = m1.multiply(3);
        assertEquals(Money.of("301.50"), product);

        Money div = Money.of("100.00").divide(new BigDecimal("3.00"));
        assertEquals(Money.of("33.33"), div); // HALF_EVEN rounding
    }

    @Test
    void testEventEnvelopeCreation() {
        EventEnvelope<String> event = EventEnvelope.create("MARKET_TICK", "SIMULATOR_ENGINE", "RELIANCE", "{\"price\": 2840.50}");
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
        assertEquals("MARKET_TICK", event.eventType());
        assertEquals("RELIANCE", event.entityId());
    }

    @Test
    void testSecurityPermissions() {
        UserPrincipal viewer = new UserPrincipal("U1", "analyst_guest", Set.of(Role.VIEWER));
        assertDoesNotThrow(() -> viewer.checkPermission(Role.VIEWER));
        assertThrows(SecurityException.class, () -> viewer.checkPermission(Role.APPROVER));

        UserPrincipal admin = new UserPrincipal("U2", "sysadmin", Set.of(Role.SIMULATOR_ADMIN));
        assertDoesNotThrow(() -> admin.checkPermission(Role.APPROVER));
    }
}
