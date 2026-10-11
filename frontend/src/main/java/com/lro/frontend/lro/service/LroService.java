package com.lro.frontend.lro.service;

import com.lro.frontend.lro.model.LroModels.*;

import java.util.List;
import java.util.Optional;

public interface LroService {
    LiquiditySnapshot getSnapshot(String bankId);
    List<AssetLiquidationMetric> getAssetLiquidationMetrics(String bankId);
    List<ModelInsight> getModelInsights();
    List<RecommendationItem> getRecommendations(String bankId);
    List<StressTestResult> runStressScenarios(String bankId);
    List<RiskAlert> getRiskAlerts(String bankId);

    // Decision support human-approval actions
    boolean approveRecommendation(String recommendationId, String officerNotes);
    boolean rejectRecommendation(String recommendationId, String reason);
    boolean acknowledgeAlert(String alertId);
}
