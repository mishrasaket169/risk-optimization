package com.lro.ml;

import com.lro.common.model.Money;
import com.lro.ml.adapter.Model1MomentumAdapter;
import com.lro.ml.adapter.Model1MomentumAdapter.MomentumPrediction;
import com.lro.ml.adapter.Model2ExecutionCostAdapter;
import com.lro.ml.adapter.Model2ExecutionCostAdapter.ExecutionEstimate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class MlAdapterUnitTests {

    @Test
    @DisplayName("Verify Prompt 21: Model 1 typed interface and fallback predictability")
    void testModel1Adapter() {
        Model1MomentumAdapter adapter = new Model1MomentumAdapter("http://127.0.0.1:9999"); // offline URL to test fallback

        MomentumPrediction pred = adapter.predict(
                "RELIANCE",
                new BigDecimal("0.015"),
                new BigDecimal("0.032"),
                new BigDecimal("0.050"),
                new BigDecimal("0.16"),
                new BigDecimal("55.0")
        );

        assertNotNull(pred);
        assertEquals("MODEL-01-MOMENTUM", pred.modelId());
        assertEquals("RELIANCE", pred.symbol());
        assertEquals("T+5 Trading Days", pred.forecastHorizon());
        assertNotNull(pred.predictedReturnPercent());
        assertTrue(pred.valuationEngine().contains("Fallback"));
    }

    @Test
    @DisplayName("Verify Prompt 22: Model 2 typed interface, units in bps and INR, and fallback")
    void testModel2Adapter() {
        Model2ExecutionCostAdapter adapter = new Model2ExecutionCostAdapter("http://127.0.0.1:9999");

        ExecutionEstimate est = adapter.estimate(
                "RELIANCE",
                "Equity",
                1000L,
                Money.of("2840.00"),
                new BigDecimal("4.00"), // 4 bps spread
                20000L,
                new BigDecimal("0.02"), // 2% ADV
                "Normal"
        );

        assertNotNull(est);
        assertEquals("MODEL-02-EXEC-COST", est.modelId());
        assertTrue(est.expectedSlippageBps().compareTo(BigDecimal.ZERO) > 0, "Slippage must be positive");
        assertTrue(est.estimatedExecutionCostINR().isPositive(), "Execution cost in INR must be positive");
        assertTrue(est.netAchievableProceedsINR().isPositive(), "Net achievable proceeds must be positive");
        assertTrue(est.completionProbability().compareTo(BigDecimal.ZERO) > 0);
    }
}
