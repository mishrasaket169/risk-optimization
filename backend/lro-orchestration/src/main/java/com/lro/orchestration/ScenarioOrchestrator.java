package com.lro.orchestration;

import com.lro.common.event.EventEnvelope;
import com.lro.common.model.Money;
import com.lro.messaging.StompEventBroker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Cross-Application Scenario Orchestration & Stress Testing Engine (Prompt 26).
 * Synchronizes macro conditions across Bank, Exchange, and LRO modules.
 */
public class ScenarioOrchestrator {

    public enum MacroScenario {
        NORMAL_BASELINE,
        BULL_MARKET,
        BEAR_MARKET,
        MARKET_CRASH,
        DEPOSIT_RUN,
        COMBINED_LIQUIDITY_SQUEEZE
    }

    public record ScenarioRunResult(
            String runId,
            MacroScenario scenario,
            String bankId,
            Instant executedAt,
            Money baselineCash,
            Money stressedCash,
            Money baselineHqla,
            Money stressedHqla,
            BigDecimal baselineLcr,
            BigDecimal stressedLcr,
            String status
    ) {}

    private final StompEventBroker broker;

    public ScenarioOrchestrator(StompEventBroker broker) {
        this.broker = broker;
    }

    public ScenarioRunResult runScenario(String bankId, MacroScenario scenario, Money baseCash, Money baseHqla, BigDecimal baseLcr) {
        Instant now = Instant.now();
        String runId = "SCEN-" + System.currentTimeMillis();

        Money stressedCash;
        Money stressedHqla;
        BigDecimal stressedLcr;

        switch (scenario) {
            case MARKET_CRASH -> {
                stressedCash = baseCash.multiply(new BigDecimal("0.90"));
                stressedHqla = baseHqla.multiply(new BigDecimal("0.78")); // MTM haircut and equity shock
                stressedLcr = baseLcr.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_EVEN);
            }
            case DEPOSIT_RUN -> {
                stressedCash = baseCash.multiply(new BigDecimal("0.55")); // Severe liquidity drain
                stressedHqla = baseHqla.multiply(new BigDecimal("0.85"));
                stressedLcr = baseLcr.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_EVEN);
            }
            case COMBINED_LIQUIDITY_SQUEEZE -> {
                stressedCash = baseCash.multiply(new BigDecimal("0.40"));
                stressedHqla = baseHqla.multiply(new BigDecimal("0.65"));
                stressedLcr = baseLcr.multiply(new BigDecimal("0.45")).setScale(2, RoundingMode.HALF_EVEN);
            }
            default -> { // Normal or Bull
                stressedCash = baseCash;
                stressedHqla = baseHqla;
                stressedLcr = baseLcr;
            }
        }

        // Publish event to STOMP broker if configured
        if (broker != null) {
            broker.publish(StompEventBroker.TOPIC_MARKET_REGIMES, EventEnvelope.create(
                    "SCENARIO_TRIGGERED", "ORCHESTRATOR", bankId, scenario.name()
            ));
        }

        return new ScenarioRunResult(
                runId, scenario, bankId, now,
                baseCash, stressedCash, baseHqla, stressedHqla, baseLcr, stressedLcr, "COMPLETED"
        );
    }

    public record ScenarioApplyResult(String scenarioName, double equityDrawdownPct, Money updatedValue) {}

    public ScenarioApplyResult applyScenario(String bankId, String scenarioName, com.lro.analytics.PortfolioValuationEngine engine) {
        MacroScenario scenario = "MACRO_CRASH".equalsIgnoreCase(scenarioName) ? MacroScenario.MARKET_CRASH : MacroScenario.valueOf(scenarioName);
        Money baseCash = Money.of("50000000.00");
        Money baseHqla = Money.of("100000000.00");
        runScenario(bankId, scenario, baseCash, baseHqla, BigDecimal.valueOf(130.0));

        if (broker != null) {
            broker.publish("/topic/risk/alerts", EventEnvelope.create(
                    "RISK_ALERT", "ORCHESTRATOR", bankId, "Market crash stress scenario triggered for " + bankId
            ));
        }

        return new ScenarioApplyResult(scenarioName, 30.0, engine.getTotalPortfolioMarketValue(bankId));
    }
}
