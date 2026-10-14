package com.lro.frontend.lro.model;

import java.math.BigDecimal;
import java.time.Instant;

public class LroModels {

    public record LiquiditySnapshot(
            String bankId,
            Instant asOfTime,
            BigDecimal cashAvailableINR,
            BigDecimal liquidAssetsHQLA_INR,
            BigDecimal projectedFundingGap1D_INR,
            BigDecimal projectedFundingGap30D_INR,
            BigDecimal educationalLcrPercent, // e.g. 142.5%
            BigDecimal educationalNsfrPercent, // e.g. 118.2%
            BigDecimal marketValueMovement24hPercent,
            String marketDataFreshnessStatus, // "Live Simulated Stream (0.2s)", "Stale"
            String modelServingStatus // "Model 1 & 2 Active", "Fallback Mode"
    ) {}

    public record AssetLiquidationMetric(
            String holdingId,
            String instrumentName,
            String category,
            BigDecimal markToMarketValueINR,
            BigDecimal haircutPercent,
            BigDecimal estimatedLiquidationProceedsINR,
            BigDecimal estimatedSlippageBps,
            BigDecimal estimatedExecutionCostINR,
            String valuationMethod, // "Order-Book Depth", "Model 2 ML Inference", "Sovereign Parity"
            String modelConfidence, // "High (92%)", "Medium (78%)", "Rule-based Fallback"
            Instant lastValuationTime
    ) {}

    public record ModelInsight(
            String modelId, // "MODEL-01-MOMENTUM", "MODEL-02-EXEC-COST"
            String modelName,
            String version,
            String targetDefinition,
            String forecastHorizon,
            String trainingDataSource,
            String validationWindow,
            String primaryMetric, // "RMSE: 0.0142" or "R²: 0.884"
            Instant lastTrainedAt,
            String driftStatus, // "Stable - No Drift", "Monitoring"
            String limitationsNotice
    ) {}

    public record RecommendationItem(
            String recommendationId,
            String bankId,
            String assetName,
            String proposedAction, // "Sell Liquid Tranche", "Overnight Tri-Party Repo", "Hold"
            long suggestedQuantity,
            BigDecimal expectedGrossProceedsINR,
            BigDecimal estimatedCostINR,
            BigDecimal netProceedsINR,
            BigDecimal liquidityRatioBenefitPercent,
            BigDecimal modelConfidencePercent,
            String rationaleText,
            String riskFactors,
            String status, // "Pending Review", "Approved by Treasury Officer", "Rejected"
            Instant generatedAt
    ) {}

    public record StressTestResult(
            String scenarioName, // "Market Crash (-25% Equities, +150bps Yield Spike)", "Deposit Run (-15% CASA Outflow)"
            BigDecimal baselineCashINR,
            BigDecimal stressedCashINR,
            BigDecimal baselineHQLA_INR,
            BigDecimal stressedHQLA_INR,
            BigDecimal baselineGapINR,
            BigDecimal stressedFundingGapINR,
            BigDecimal baselineLcrPercent,
            BigDecimal stressedLcrPercent,
            String severityLevel // "Critical", "Moderate", "Manageable"
    ) {}

    public record RiskAlert(
            String alertId,
            String severity, // "CRITICAL", "WARNING", "INFO"
            String title,
            String description,
            Instant triggeredAt,
            boolean acknowledged
    ) {}
}
