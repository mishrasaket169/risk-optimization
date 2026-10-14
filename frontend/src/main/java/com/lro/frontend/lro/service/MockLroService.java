package com.lro.frontend.lro.service;

import com.lro.frontend.lro.model.LroModels.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockLroService implements LroService {

    private final Map<String, List<RecommendationItem>> recommendations = new ConcurrentHashMap<>();
    private final Map<String, List<RiskAlert>> alerts = new ConcurrentHashMap<>();
    private final List<ModelInsight> insights = new ArrayList<>();

    public MockLroService() {
        seedInsights();
        seedBankSpecifics();
    }

    private void seedInsights() {
        Instant now = Instant.now();

        insights.add(new ModelInsight(
                "MODEL-01-MOMENTUM",
                "XGBoost Short-Horizon Equity Momentum",
                "v2.1.0-prod",
                "Predicts 5-session forward return sign & magnitude",
                "T+5 Trading Days",
                "Simulated Exchange Historical OHLCV & Technical Indicators (RSI, MACD, Volume Delta)",
                "2023-01-01 to 2026-06-30 (Synthetic)",
                "Directional Accuracy: 64.8%, RMSE: 0.0182",
                now.minusSeconds(86400 * 5),
                "Stable - No Drift",
                "Decision support signal only. Not an instruction to execute automated trades. Does not account for unscheduled macro geopolitical shocks."
        ));

        insights.add(new ModelInsight(
                "MODEL-02-EXEC-COST",
                "XGBoost Liquidity & Execution Cost Estimator",
                "v1.4.2-prod",
                "Estimates slippage (bps) and achievable liquidation value under order-book impact",
                "Immediate / Intraday Liquidation",
                "Simulated Continuous Double Auction Order Book depth, historical spreads and order sizes",
                "2024-01-01 to 2026-08-31 (Synthetic Matching)",
                "R²: 0.892, Mean Absolute Error: 3.4 bps",
                now.minusSeconds(86400 * 3),
                "Stable - Normal Regime",
                "Trained strictly on synthetic exchange mechanics; does not represent live broker execution guarantees. Extreme lot sizes trigger conservative fallback estimation."
        ));
    }

    private void seedBankSpecifics() {
        Instant now = Instant.now();

        String[] bankIds = {"BANK-01-LRG", "BANK-02-MED", "BANK-03-SML", "BANK-04-DST"};
        for (String bId : bankIds) {
            boolean isDistressed = bId.equals("BANK-04-DST");

            List<RecommendationItem> recList = new ArrayList<>();
            recList.add(new RecommendationItem(
                    bId + "-REC-01",
                    bId,
                    "91-Day Sovereign Treasury Bill",
                    "Overnight Tri-Party Repo (TREPS)",
                    250000L,
                    new BigDecimal("24600000.00"),
                    new BigDecimal("12000.00"),
                    new BigDecimal("24588000.00"),
                    new BigDecimal("4.25"),
                    new BigDecimal("96.50"),
                    "T-Bills carry zero regulatory haircut under RBI guidelines. Overnight TREPS raises immediate cash without parting with bond ownership.",
                    "Low risk; repo rate rollover variance.",
                    "Pending Review",
                    now.minusSeconds(1800)
            ));

            recList.add(new RecommendationItem(
                    bId + "-REC-02",
                    bId,
                    "7.18% GS 2033 Sovereign Benchmark",
                    "Bilateral Interbank Repo",
                    200000L,
                    new BigDecimal("20168000.00"),
                    new BigDecimal("18500.00"),
                    new BigDecimal("20149500.00"),
                    new BigDecimal("3.10"),
                    new BigDecimal("94.20"),
                    "High-liquidity sovereign tranche. Tight bid-ask spread of 4 bps minimizes liquidation impact.",
                    "Counterparty settlement timing.",
                    "Pending Review",
                    now.minusSeconds(3600)
            ));

            if (isDistressed) {
                recList.add(new RecommendationItem(
                        bId + "-REC-03",
                        bId,
                        "Reliance Industries Eq (Simulated)",
                        "Tranche Sale via Limit Order",
                        20000L,
                        new BigDecimal("56810000.00"),
                        new BigDecimal("284000.00"),
                        new BigDecimal("56526000.00"),
                        new BigDecimal("8.40"),
                        new BigDecimal("82.00"),
                        "Model 1 indicates declining 5-day momentum (-1.4%). Model 2 predicts execution cost of 50 bps if executed across 3 algorithmic slices.",
                        "Equity market volatility; impact on capital gains.",
                        "Pending Review",
                        now.minusSeconds(600)
                ));
            }
            recommendations.put(bId, recList);

            List<RiskAlert> alertList = new ArrayList<>();
            if (isDistressed) {
                alertList.add(new RiskAlert(bId + "-ALT-01", "CRITICAL", "Projected 1-Day Funding Deficit", "Net projected outflow in the 1-Day bucket exceeds unencumbered cash buffer by ₹ 540 Cr.", now.minusSeconds(7200), false));
                alertList.add(new RiskAlert(bId + "-ALT-02", "WARNING", "Gross NPA Ratio Exceeds Threshold", "NPA ratio reached 14.80%, triggering enhanced provision requirements under Prompt Corrective Action (PCA).", now.minusSeconds(14400), false));
            } else {
                alertList.add(new RiskAlert(bId + "-ALT-01", "INFO", "Quarterly LCR Buffer Optimal", "Educational LCR estimated at healthy 142.5%, comfortably exceeding regulatory 100% baseline.", now.minusSeconds(7200), true));
            }
            alerts.put(bId, alertList);
        }
    }

    @Override
    public LiquiditySnapshot getSnapshot(String bankId) {
        boolean isDistressed = "BANK-04-DST".equalsIgnoreCase(bankId);
        return new LiquiditySnapshot(
                bankId,
                Instant.now(),
                isDistressed ? new BigDecimal("3100000000.00") : new BigDecimal("42000000000.00"),
                isDistressed ? new BigDecimal("14200000000.00") : new BigDecimal("124000000000.00"),
                isDistressed ? new BigDecimal("-400000000.00") : new BigDecimal("30000000.00"),
                isDistressed ? new BigDecimal("-1800000000.00") : new BigDecimal("2400000000.00"),
                isDistressed ? new BigDecimal("82.40") : new BigDecimal("142.50"), // Distressed breaches 100% LCR
                isDistressed ? new BigDecimal("91.20") : new BigDecimal("118.20"),
                isDistressed ? new BigDecimal("-1.85") : new BigDecimal("0.42"),
                "Live Simulated Stream (0.2s)",
                "Model 1 & 2 Active"
        );
    }

    @Override
    public List<AssetLiquidationMetric> getAssetLiquidationMetrics(String bankId) {
        Instant now = Instant.now();
        List<AssetLiquidationMetric> list = new ArrayList<>();

        list.add(new AssetLiquidationMetric(bankId + "-TRS-01", "7.18% GS 2033 (Govt of India)", "G-Sec", new BigDecimal("50420000.00"), new BigDecimal("0.50"), new BigDecimal("50167900.00"), new BigDecimal("4.2"), new BigDecimal("21176.40"), "Sovereign Parity", "High (95%)", now));
        list.add(new AssetLiquidationMetric(bankId + "-TRS-02", "7.06% GS 2028 (Govt of India)", "G-Sec", new BigDecimal("30150000.00"), new BigDecimal("0.50"), new BigDecimal("29999250.00"), new BigDecimal("4.5"), new BigDecimal("13567.50"), "Sovereign Parity", "High (95%)", now));
        list.add(new AssetLiquidationMetric(bankId + "-TRS-03", "91-Day Treasury Bill", "T-Bill", new BigDecimal("39580000.00"), BigDecimal.ZERO, new BigDecimal("39580000.00"), new BigDecimal("2.1"), new BigDecimal("8311.80"), "Sovereign Parity", "High (98%)", now));
        list.add(new AssetLiquidationMetric(bankId + "-TRS-05", "8.15% HDFC Bank AAA Bond 2029", "Corporate Bond", new BigDecimal("99800000.00"), new BigDecimal("5.00"), new BigDecimal("94810000.00"), new BigDecimal("18.4"), new BigDecimal("183632.00"), "Model 2 ML Inference", "Medium (84%)", now));
        list.add(new AssetLiquidationMetric(bankId + "-TRS-06", "Reliance Industries Eq (Simulated)", "Equity", new BigDecimal("68200000.00"), new BigDecimal("20.00"), new BigDecimal("54560000.00"), new BigDecimal("48.5"), new BigDecimal("330770.00"), "Model 2 ML Inference", "High (91%)", now));

        return list;
    }

    @Override
    public List<ModelInsight> getModelInsights() {
        return Collections.unmodifiableList(insights);
    }

    @Override
    public List<RecommendationItem> getRecommendations(String bankId) {
        return recommendations.getOrDefault(bankId, List.of());
    }

    @Override
    public List<StressTestResult> runStressScenarios(String bankId) {
        boolean isDistressed = "BANK-04-DST".equalsIgnoreCase(bankId);
        List<StressTestResult> results = new ArrayList<>();

        results.add(new StressTestResult(
                "Market Crash (-25% Equities, +150bps G-Sec Yield)",
                new BigDecimal("42000000000.00"),
                new BigDecimal("39500000000.00"),
                new BigDecimal("124000000000.00"),
                new BigDecimal("112000000000.00"),
                new BigDecimal("30000000.00"),
                new BigDecimal("-850000000.00"),
                new BigDecimal("142.50"),
                isDistressed ? new BigDecimal("65.20") : new BigDecimal("112.40"),
                isDistressed ? "Critical" : "Moderate"
        ));

        results.add(new StressTestResult(
                "Deposit Run (-15% CASA Outflow over 30 Days)",
                new BigDecimal("42000000000.00"),
                new BigDecimal("21000000000.00"),
                new BigDecimal("124000000000.00"),
                new BigDecimal("105000000000.00"),
                new BigDecimal("30000000.00"),
                new BigDecimal("-3400000000.00"),
                new BigDecimal("142.50"),
                isDistressed ? new BigDecimal("54.10") : new BigDecimal("98.50"),
                "Critical"
        ));

        results.add(new StressTestResult(
                "Combined Liquidity Squeeze (Freeze on Interbank Lending)",
                new BigDecimal("42000000000.00"),
                new BigDecimal("16500000000.00"),
                new BigDecimal("124000000000.00"),
                new BigDecimal("98000000000.00"),
                new BigDecimal("30000000.00"),
                new BigDecimal("-5200000000.00"),
                new BigDecimal("142.50"),
                isDistressed ? new BigDecimal("42.80") : new BigDecimal("86.20"),
                "Critical"
        ));

        return results;
    }

    @Override
    public List<RiskAlert> getRiskAlerts(String bankId) {
        return alerts.getOrDefault(bankId, List.of());
    }

    @Override
    public boolean approveRecommendation(String recommendationId, String officerNotes) {
        for (List<RecommendationItem> list : recommendations.values()) {
            for (int i = 0; i < list.size(); i++) {
                RecommendationItem r = list.get(i);
                if (r.recommendationId().equalsIgnoreCase(recommendationId)) {
                    list.set(i, new RecommendationItem(
                            r.recommendationId(), r.bankId(), r.assetName(), r.proposedAction(),
                            r.suggestedQuantity(), r.expectedGrossProceedsINR(), r.estimatedCostINR(),
                            r.netProceedsINR(), r.liquidityRatioBenefitPercent(), r.modelConfidencePercent(),
                            r.rationaleText() + " [Approved: " + officerNotes + "]", r.riskFactors(),
                            "Approved by Treasury Officer", r.generatedAt()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean rejectRecommendation(String recommendationId, String reason) {
        for (List<RecommendationItem> list : recommendations.values()) {
            for (int i = 0; i < list.size(); i++) {
                RecommendationItem r = list.get(i);
                if (r.recommendationId().equalsIgnoreCase(recommendationId)) {
                    list.set(i, new RecommendationItem(
                            r.recommendationId(), r.bankId(), r.assetName(), r.proposedAction(),
                            r.suggestedQuantity(), r.expectedGrossProceedsINR(), r.estimatedCostINR(),
                            r.netProceedsINR(), r.liquidityRatioBenefitPercent(), r.modelConfidencePercent(),
                            r.rationaleText() + " [Rejected: " + reason + "]", r.riskFactors(),
                            "Rejected", r.generatedAt()
                    ));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean acknowledgeAlert(String alertId) {
        for (List<RiskAlert> list : alerts.values()) {
            for (int i = 0; i < list.size(); i++) {
                RiskAlert a = list.get(i);
                if (a.alertId().equalsIgnoreCase(alertId)) {
                    list.set(i, new RiskAlert(a.alertId(), a.severity(), a.title(), a.description(), a.triggeredAt(), true));
                    return true;
                }
            }
        }
        return false;
    }
}
