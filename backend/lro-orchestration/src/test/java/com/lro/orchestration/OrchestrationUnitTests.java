package com.lro.orchestration;

import com.lro.common.model.Money;
import com.lro.common.security.SecurityContext.Role;
import com.lro.common.security.SecurityContext.UserPrincipal;
import com.lro.ml.adapter.Model1MomentumAdapter;
import com.lro.ml.adapter.Model2ExecutionCostAdapter;
import com.lro.orchestration.RecommendationRankingEngine.ApprovalStatus;
import com.lro.orchestration.RecommendationRankingEngine.RankedRecommendation;
import com.lro.orchestration.ScenarioOrchestrator.MacroScenario;
import com.lro.orchestration.ScenarioOrchestrator.ScenarioRunResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class OrchestrationUnitTests {

    @Test
    @DisplayName("Verify Prompt 23 & 24: Recommendation ranking and Maker/Checker dual-approval workflow")
    void testRecommendationWorkflow() {
        Model1MomentumAdapter m1 = new Model1MomentumAdapter("http://127.0.0.1:9999");
        Model2ExecutionCostAdapter m2 = new Model2ExecutionCostAdapter("http://127.0.0.1:9999");
        RecommendationRankingEngine engine = new RecommendationRankingEngine(m1, m2);

        RankedRecommendation rec = engine.generateRecommendation(
                "BANK-01-LRG", "TBILL-91D", "T-Bill", 100000L, Money.of("98.32"),
                "Overnight Tri-Party Repo", "Zero regulatory haircut asset"
        );

        assertNotNull(rec);
        assertEquals(ApprovalStatus.PENDING_REVIEW, rec.status());

        UserPrincipal analyst = new UserPrincipal("U-01", "tr_analyst", Set.of(Role.TREASURY_ANALYST));
        UserPrincipal approver = new UserPrincipal("U-02", "chief_approver", Set.of(Role.APPROVER));
        UserPrincipal viewer = new UserPrincipal("U-03", "guest_viewer", Set.of(Role.VIEWER));

        // Viewer cannot approve
        assertThrows(SecurityException.class, () -> engine.approveMaker(rec.recommendationId(), viewer));

        // 1. Maker approves
        boolean makerOk = engine.approveMaker(rec.recommendationId(), analyst);
        assertTrue(makerOk);
        assertEquals(ApprovalStatus.MAKER_RECOMMENDED, engine.getRecommendations("BANK-01-LRG").get(0).status());

        // 2. Checker authorizes
        boolean checkerOk = engine.authorizeChecker(rec.recommendationId(), approver);
        assertTrue(checkerOk);
        assertEquals(ApprovalStatus.CHECKER_AUTHORIZED, engine.getRecommendations("BANK-01-LRG").get(0).status());
    }

    @Test
    @DisplayName("Verify Prompt 26: Cross-application scenario orchestration under Market Crash")
    void testScenarioOrchestration() {
        ScenarioOrchestrator orchestrator = new ScenarioOrchestrator(null);

        ScenarioRunResult result = orchestrator.runScenario(
                "BANK-01-LRG",
                MacroScenario.MARKET_CRASH,
                Money.of("100000000.00"),
                Money.of("500000000.00"),
                new BigDecimal("140.00")
        );

        assertEquals("COMPLETED", result.status());
        assertTrue(result.stressedCash().compareTo(result.baselineCash()) < 0, "Stressed cash must decrease");
        assertTrue(result.stressedHqla().compareTo(result.baselineHqla()) < 0, "Stressed HQLA must decrease under crash");
        assertTrue(result.stressedLcr().compareTo(result.baselineLcr()) < 0, "LCR must decline under market stress");
    }
}
