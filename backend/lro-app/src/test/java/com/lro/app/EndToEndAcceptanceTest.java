package com.lro.app;

import com.lro.analytics.LiquidityMetricsEngine;
import com.lro.analytics.PortfolioValuationEngine;
import com.lro.analytics.PortfolioValuationEngine.PortfolioLot;
import com.lro.bank.service.BankPopulationGenerator;
import com.lro.bank.service.BankPopulationGenerator.SyntheticBankPopulation;
import com.lro.bank.service.DoubleEntryLedgerService;
import com.lro.bank.service.DoubleEntryLedgerService.JournalEntry;
import com.lro.common.model.Money;
import com.lro.exchange.service.ContinuousMatchingEngine;
import com.lro.exchange.service.ContinuousMatchingEngine.Order;
import com.lro.exchange.service.ContinuousMatchingEngine.OrderType;
import com.lro.exchange.service.ContinuousMatchingEngine.Side;
import com.lro.exchange.service.ContinuousMatchingEngine.TradeExecution;
import com.lro.exchange.service.ExchangeInstrumentCatalogue;
import com.lro.messaging.StompEventBroker;
import com.lro.ml.adapter.Model1MomentumAdapter;
import com.lro.ml.adapter.Model2ExecutionCostAdapter;
import com.lro.orchestration.RecommendationRankingEngine;
import com.lro.orchestration.RecommendationRankingEngine.ApprovalStatus;
import com.lro.orchestration.RecommendationRankingEngine.RankedRecommendation;
import com.lro.orchestration.ScenarioOrchestrator;
import com.lro.persistence.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PROMPT 27: End-to-End Acceptance Test Across Full Ecosystem")
class EndToEndAcceptanceTest {

    private DatabaseManager dbManager;
    private StompEventBroker broker;
    private DoubleEntryLedgerService ledgerService;
    private ExchangeInstrumentCatalogue catalogue;
    private ContinuousMatchingEngine matchingEngine;
    private PortfolioValuationEngine valuationEngine;
    private LiquidityMetricsEngine liquidityMetricsEngine;
    private Model1MomentumAdapter model1Adapter;
    private Model2ExecutionCostAdapter model2Adapter;
    private RecommendationRankingEngine rankingEngine;
    private ScenarioOrchestrator scenarioOrchestrator;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager();
        broker = new StompEventBroker();
        ledgerService = new DoubleEntryLedgerService();
        catalogue = new ExchangeInstrumentCatalogue();
        matchingEngine = new ContinuousMatchingEngine();
        valuationEngine = new PortfolioValuationEngine(broker);
        liquidityMetricsEngine = new LiquidityMetricsEngine();
        model1Adapter = new Model1MomentumAdapter();
        model2Adapter = new Model2ExecutionCostAdapter();
        rankingEngine = new RecommendationRankingEngine(model1Adapter, model2Adapter);
        scenarioOrchestrator = new ScenarioOrchestrator(broker);
    }

    @Test
    @DisplayName("E2E Phase 1: Database schema migrations apply cleanly across 4 bank schemas & topologies")
    void testDatabaseMigrationAndBankSeeding() {
        boolean migrated = dbManager.runMigrations();
        assertTrue(migrated, "Flyway migrations V1, V2, V3 must succeed");

        SyntheticBankPopulation large = BankPopulationGenerator.generatePopulation("BANK-01-LRG", 42L);
        SyntheticBankPopulation medium = BankPopulationGenerator.generatePopulation("BANK-02-MED", 42L);
        SyntheticBankPopulation small = BankPopulationGenerator.generatePopulation("BANK-03-SML", 42L);
        SyntheticBankPopulation distressed = BankPopulationGenerator.generatePopulation("BANK-04-DST", 42L);

        assertEquals(10000, large.customerCount());
        assertEquals(7500, medium.customerCount());
        assertEquals(5000, small.customerCount());
        assertEquals(5000, distressed.customerCount());

        assertTrue(distressed.grossNpaRatio().compareTo(large.grossNpaRatio()) > 0,
                "Distressed bank must have higher synthetic NPA ratio (" + distressed.grossNpaRatio() + "% vs " + large.grossNpaRatio() + "%)");
        assertTrue(large.cashReserve().isGreaterThan(distressed.cashReserve()),
                "Large bank must possess larger cash reserve than distressed bank");
    }

    @Test
    @DisplayName("E2E Phase 2: Double-entry ledger invariant strictly enforced (Debits == Credits)")
    void testDoubleEntryLedgerBalancing() {
        Money amount = Money.of("50000000.00"); // 5 Crore INR
        String txnId = "TXN-E2E-001";

        List<JournalEntry> balancedEntries = List.of(
                new JournalEntry("E1", txnId, Instant.now(), "CASH_RESERVE", amount, Money.ZERO, "Cash Deposit", "POSTED"),
                new JournalEntry("E2", txnId, Instant.now(), "CUSTOMER_DEPOSIT", Money.ZERO, amount, "Deposit Liability", "POSTED")
        );

        assertDoesNotThrow(() -> ledgerService.postTransaction("BANK-01-LRG", txnId, "Deposit Inflow", balancedEntries));

        // Negative test: Unbalanced entry must fail
        List<JournalEntry> unbalancedEntries = List.of(
                new JournalEntry("E3", "TXN-FAIL-01", Instant.now(), "CASH_RESERVE", amount, Money.ZERO, "Unbalanced Cash", "POSTED"),
                new JournalEntry("E4", "TXN-FAIL-01", Instant.now(), "CUSTOMER_DEPOSIT", Money.ZERO, Money.of("40000000.00"), "Mismatched Credit", "POSTED")
        );

        assertThrows(com.lro.common.exception.ValidationException.class, () ->
                ledgerService.postTransaction("BANK-01-LRG", "TXN-FAIL-01", "Unbalanced Inflow", unbalancedEntries)
        );
    }

    @Test
    @DisplayName("E2E Phase 3: Continuous matching engine executes double auction with price-time priority")
    void testOrderMatchingAndTradeGeneration() {
        // Submit Sell Limit order (Ask)
        Order ask = new Order("ASK-01", "RELIANCE", Side.SELL, OrderType.LIMIT, 500, Money.of("2850.00"));
        matchingEngine.submitOrder(ask);

        // Submit matching Buy Limit order (Bid) with partial quantity
        Order bid = new Order("BID-01", "RELIANCE", Side.BUY, OrderType.LIMIT, 200, Money.of("2850.00"));
        List<TradeExecution> trades = matchingEngine.submitOrder(bid);

        assertEquals(1, trades.size());
        TradeExecution trade = trades.get(0);
        assertEquals(200, trade.quantity());
        assertEquals(Money.of("2850.00"), trade.executionPrice());
        assertEquals("RELIANCE", trade.symbol());

        // Verify remaining book depth has 300 remaining on ask
        var book = matchingEngine.getOrderBook("RELIANCE");
        assertNotNull(book);
        assertFalse(book.asks().isEmpty());
        assertEquals(300, book.asks().get(0).quantity());
    }

    @Test
    @DisplayName("E2E Phase 4: Bank portfolio MTM valuation updates dynamically on trade settlement")
    void testPortfolioValuationAndMarkToMarket() {
        String bankId = "BANK-01-LRG";
        PortfolioLot equityLot = new PortfolioLot(
                "LOT-E2E-01", bankId, "RELIANCE", "Equity", 10000L,
                Money.of("2800.00"), Money.of("2800.00"), false, Instant.now()
        );
        valuationEngine.addLot(equityLot);

        Money initialMtm = valuationEngine.getTotalPortfolioMarketValue(bankId);
        assertEquals(Money.of("28000000.00"), initialMtm); // 10,000 * 2,800 = 2.8 Crore

        // Revalue lot at new market price 2,850.00
        valuationEngine.updateLotMarketPrice("LOT-E2E-01", Money.of("2850.00"));

        Money updatedMtm = valuationEngine.getTotalPortfolioMarketValue(bankId);
        assertEquals(Money.of("28500000.00"), updatedMtm); // Gain of 5 Lakhs
        assertTrue(updatedMtm.isGreaterThan(initialMtm));
    }

    @Test
    @DisplayName("E2E Phase 5: Liquidity metrics calculate HQLA haircuts, illustrative LCR, and 1-day 99% VaR")
    void testLiquidityMetricsCalculation() {
        Money cash = Money.of("100000000.00");      // 10 Cr (0% haircut -> 10 Cr)
        Money gsec = Money.of("200000000.00");      // 20 Cr (0% haircut -> 20 Cr)
        Money corpBond = Money.of("50000000.00");   // 5 Cr (15% haircut -> 4.25 Cr)
        Money equities = Money.of("40000000.00");   // 4 Cr (50% haircut -> 2 Cr)

        Money totalHqla = liquidityMetricsEngine.calculateTotalHqla(cash, gsec, corpBond, equities);
        // 10 + 20 + 4.25 + 2 = 36.25 Cr
        assertEquals(Money.of("362500000.00"), totalHqla);

        // Illustrative 30-day net cash outflow: 25 Cr
        Money netOutflows = Money.of("250000000.00");
        double lcr = liquidityMetricsEngine.calculateIllustrativeLcr(totalHqla, netOutflows);
        // 36.25 / 25 * 100 = 145%
        assertEquals(145.0, lcr, 0.01);
        assertTrue(lcr >= 100.0, "Bank meets illustrative 100% LCR requirement");

        // 1-Day 99% Parametric VaR (Z=2.3263, daily vol 1.5%)
        Money portfolioVal = Money.of("100000000.00");
        Money var99 = liquidityMetricsEngine.calculateParametricVaR(portfolioVal, 0.015, 2.3263);
        // 10 Cr * 0.015 * 2.3263 = 3,489,450
        assertEquals(Money.of("3489450.00"), var99);
    }

    @Test
    @DisplayName("E2E Phase 6: Decision Support recommendation ranking with Model 1 & 2 + Maker/Checker approval")
    void testRecommendationRankingAndMakerCheckerWorkflow() {
        RankedRecommendation rec = rankingEngine.generateRecommendation(
                "BANK-01-LRG", "GS-7.18-2033", "G-Sec", 500000L, Money.of("100.85"),
                "RBI Standing Liquidity Facility (SLF)", "High liquidity sovereign tranche"
        );

        assertNotNull(rec);
        assertEquals(ApprovalStatus.PENDING_REVIEW, rec.status());
        assertTrue(rec.utilityScore() > 0.0, "Utility score must be positive");
        assertNotNull(rec.momentumPrediction());
        assertNotNull(rec.costPrediction());

        // Maker attempts to execute directly -> Fails
        assertThrows(IllegalStateException.class, () ->
                rankingEngine.executeRecommendation(rec.recommendationId(), "MAKER_USER")
        );

        // Checker approves recommendation
        rankingEngine.approveRecommendation(rec.recommendationId(), "CHECKER_SUPERVISOR");
        var approvedRec = rankingEngine.getRecommendation(rec.recommendationId());
        assertEquals(ApprovalStatus.APPROVED, approvedRec.status());
        assertEquals("CHECKER_SUPERVISOR", approvedRec.checkerUserId());

        // Execution succeeds
        assertDoesNotThrow(() ->
                rankingEngine.executeRecommendation(rec.recommendationId(), "EXEC_OPERATOR")
        );
        var executedRec = rankingEngine.getRecommendation(rec.recommendationId());
        assertEquals(ApprovalStatus.EXECUTED, executedRec.status());
    }

    @Test
    @DisplayName("E2E Phase 7: Macro Crash stress scenario shock triggers portfolio drawdown and risk alert")
    void testMacroCrashScenarioShock() {
        AtomicBoolean alertReceived = new AtomicBoolean(false);
        broker.subscribe("/topic/risk/alerts", "test-client", envelope -> {
            alertReceived.set(true);
        });

        // Initialize bank portfolio
        valuationEngine.addLot(new PortfolioLot(
                "LOT-CRASH-01", "BANK-01-LRG", "RELIANCE", "Equity", 50000L,
                Money.of("2800.00"), Money.of("2800.00"), false, Instant.now()
        ));

        // Trigger Macro Crash Scenario (30% equity decline, 150 bps rate hike, 15% deposit run-off)
        var result = scenarioOrchestrator.applyScenario("BANK-01-LRG", "MACRO_CRASH", valuationEngine);

        assertNotNull(result);
        assertEquals("MACRO_CRASH", result.scenarioName());
        assertTrue(result.equityDrawdownPct() >= 30.0);
        assertTrue(alertReceived.get(), "Risk alert must be broadcast to STOMP broker on scenario shock");
    }

    @Test
    @DisplayName("E2E Phase 8: Idempotency and negative failure paths handled gracefully")
    void testIdempotencyAndFailurePaths() {
        // Duplicate order ID must not crash matching engine
        Order order1 = new Order("ORDER-DUP-01", "TCS", Side.BUY, OrderType.LIMIT, 100, Money.of("3800.00"));
        matchingEngine.submitOrder(order1);

        // Re-submitting identical order ID
        assertThrows(IllegalArgumentException.class, () -> matchingEngine.submitOrder(order1));

        // Model fallbacks operate deterministically if remote service is unreachable
        var fallbackMomentum = model1Adapter.predictMomentum("NON_EXISTENT_SYMBOL", List.of(BigDecimal.valueOf(100.0)));
        assertNotNull(fallbackMomentum);
        assertTrue(fallbackMomentum.confidence() > 0.0);
        assertTrue(fallbackMomentum.provenance().contains("Fallback"));

        var fallbackCost = model2Adapter.estimateCost("NON_EXISTENT_SYMBOL", 50000, Money.of("100.00"));
        assertNotNull(fallbackCost);
        assertTrue(fallbackCost.slippageBps() > 0.0);
        assertTrue(fallbackCost.provenance().contains("Fallback"));
    }
}
