package com.lro.analytics;

import com.lro.analytics.LiquidityMetricsEngine.HqlaCategory;
import com.lro.analytics.PortfolioValuationEngine.PortfolioLot;
import com.lro.common.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyticsUnitTests {

    @Test
    @DisplayName("Verify Prompt 19: Portfolio MTM revaluation updates lot values and unrealized P&L")
    void testPortfolioValuation() {
        PortfolioValuationEngine engine = new PortfolioValuationEngine(null);

        PortfolioLot lot = new PortfolioLot(
                "LOT-01", "BANK-01-LRG", "RELIANCE", "Equity",
                1000L, Money.of("2800.00"), Money.of("2800.00"), false, Instant.now()
        );
        engine.addLot(lot);

        assertEquals(Money.of("2800000.00"), engine.getTotalPortfolioMarketValue("BANK-01-LRG"));

        // Price increases to ₹ 2850.00
        engine.updateMarketPrice("RELIANCE", Money.of("2850.00"));
        assertEquals(Money.of("2850000.00"), engine.getTotalPortfolioMarketValue("BANK-01-LRG"));

        PortfolioLot updated = engine.getHoldings("BANK-01-LRG").get(0);
        assertEquals(Money.of("50000.00"), updated.getUnrealizedGainLoss());
    }

    @Test
    @DisplayName("Verify Prompt 20: HQLA haircuts, educational LCR, and Parametric VaR")
    void testLiquidityMetricsAndVaR() {
        Money marketVal = Money.of("10000000.00"); // ₹ 1 Cr

        // Level 1: 0.5% haircut -> 99.5% proceeds
        Money l1Proceeds = LiquidityMetricsEngine.calculateLiquidationProceeds(marketVal, LiquidityMetricsEngine.getRegulatoryHaircut(HqlaCategory.LEVEL_1));
        assertEquals(Money.of("9950000.00"), l1Proceeds);

        // Level 2B: 50% haircut -> 50% proceeds
        Money l2bProceeds = LiquidityMetricsEngine.calculateLiquidationProceeds(marketVal, LiquidityMetricsEngine.getRegulatoryHaircut(HqlaCategory.LEVEL_2B));
        assertEquals(Money.of("5000000.00"), l2bProceeds);

        // Educational LCR
        Money hqla = Money.of("14000000.00");
        Money outflows = Money.of("10000000.00");
        BigDecimal lcr = LiquidityMetricsEngine.calculateEducationalLcr(hqla, outflows);
        assertEquals(new BigDecimal("140.00"), lcr);

        // Parametric VaR 99%
        Money var = LiquidityMetricsEngine.calculateParametricVaR99(marketVal, new BigDecimal("0.16")); // 16% vol
        assertTrue(var.getAmount().doubleValue() > 200000.0);
    }
}
