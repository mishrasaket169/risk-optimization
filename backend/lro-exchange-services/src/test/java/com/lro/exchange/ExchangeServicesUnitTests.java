package com.lro.exchange;

import com.lro.common.model.Money;
import com.lro.exchange.service.ContinuousMatchingEngine;
import com.lro.exchange.service.ContinuousMatchingEngine.*;
import com.lro.exchange.service.ExchangeInstrumentCatalogue;
import com.lro.exchange.service.ExchangeInstrumentCatalogue.Instrument;
import com.lro.exchange.service.MarketRegimeEngine;
import com.lro.exchange.service.MarketRegimeEngine.Regime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeServicesUnitTests {

    @Test
    @DisplayName("Verify Prompt 16: Instrument catalogue spans Indian market families")
    void testInstrumentCatalogue() {
        ExchangeInstrumentCatalogue cat = new ExchangeInstrumentCatalogue();

        Optional<Instrument> eq = cat.getInstrument("RELIANCE");
        assertTrue(eq.isPresent());
        assertEquals("Equity", eq.get().category());

        Optional<Instrument> gsec = cat.getInstrument("GS-7.18-2033");
        assertTrue(gsec.isPresent());
        assertEquals("G-Sec", gsec.get().category());
        assertNotNull(gsec.get().couponOrYieldPct());

        Optional<Instrument> fut = cat.getInstrument("NIFTY-FUT-1M");
        assertTrue(fut.isPresent());
        assertEquals("Futures", fut.get().category());
    }

    @Test
    @DisplayName("Verify Prompt 17: Matching engine matches Limit orders with Price-Time priority")
    void testContinuousMatchingEngine() {
        ContinuousMatchingEngine engine = new ContinuousMatchingEngine();

        // 1. Place resting Ask (Sell 100 @ 2840.00)
        Order ask1 = new Order("O-SELL-1", "RELIANCE", Side.SELL, OrderType.LIMIT, 100, Money.of("2840.00"));
        List<TradeExecution> fills1 = engine.submitOrder(ask1);
        assertTrue(fills1.isEmpty(), "Resting ask produces no instant fills");
        assertEquals(OrderStatus.ACCEPTED, ask1.getStatus());

        // 2. Place incoming Buy (Buy 60 @ 2840.00) -> should match 60 shares
        Order buy1 = new Order("O-BUY-1", "RELIANCE", Side.BUY, OrderType.LIMIT, 60, Money.of("2840.00"));
        List<TradeExecution> fills2 = engine.submitOrder(buy1);

        assertEquals(1, fills2.size());
        TradeExecution trade = fills2.get(0);
        assertEquals(60, trade.quantity());
        assertEquals(Money.of("2840.00"), trade.executionPrice());
        assertEquals(OrderStatus.FILLED, buy1.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, ask1.getStatus());
        assertEquals(40, ask1.getRemainingQuantity());
    }

    @Test
    @DisplayName("Verify Prompt 17: Market regime adjustments during Crash conditions")
    void testMarketRegimes() {
        MarketRegimeEngine engine = new MarketRegimeEngine();
        assertEquals(Regime.NORMAL, engine.getRegime());
        assertEquals(BigDecimal.ONE, engine.getSpreadMultiplier());

        engine.setRegime(Regime.CRASH);
        assertEquals(Regime.CRASH, engine.getRegime());
        assertTrue(engine.getSpreadMultiplier().compareTo(new BigDecimal("3.0")) > 0, "Crash regime must widen spreads > 3x");
    }
}
