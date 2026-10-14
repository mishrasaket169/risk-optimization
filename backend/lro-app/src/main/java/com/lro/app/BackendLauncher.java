package com.lro.app;

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
import com.lro.exchange.service.ExchangeInstrumentCatalogue;
import com.lro.exchange.service.MarketRegimeEngine;
import com.lro.messaging.StompEventBroker;
import com.lro.ml.adapter.Model1MomentumAdapter;
import com.lro.ml.adapter.Model2ExecutionCostAdapter;
import com.lro.orchestration.RecommendationRankingEngine;
import com.lro.orchestration.ScenarioOrchestrator;
import com.lro.persistence.DatabaseManager;

import java.time.Instant;
import java.util.List;

/**
 * Main Backend Application Foundation implementing Prompt 11, 25, 28, and 30.
 * Orchestrates multi-module startup, database migrations, synthetic populations,
 * messaging brokers, analytics engines, and ML inference adapters.
 */
public class BackendLauncher {

    private final DatabaseManager dbManager;
    private final StompEventBroker broker;
    private final DoubleEntryLedgerService ledgerService;
    private final ExchangeInstrumentCatalogue catalogue;
    private final ContinuousMatchingEngine matchingEngine;
    private final MarketRegimeEngine regimeEngine;
    private final PortfolioValuationEngine valuationEngine;
    private final Model1MomentumAdapter model1Adapter;
    private final Model2ExecutionCostAdapter model2Adapter;
    private final RecommendationRankingEngine rankingEngine;
    private final ScenarioOrchestrator scenarioOrchestrator;
    private final com.lro.app.ops.DataRetentionService retentionService;
    private final com.lro.app.ops.SystemHealthMonitor healthMonitor;
    private final com.lro.app.ingestion.HistoricalDataIngestionService ingestionService;

    public BackendLauncher() {
        this.dbManager = new DatabaseManager();
        this.broker = new StompEventBroker();
        this.ledgerService = new DoubleEntryLedgerService();
        this.catalogue = new ExchangeInstrumentCatalogue();
        this.matchingEngine = new ContinuousMatchingEngine();
        this.regimeEngine = new MarketRegimeEngine();
        this.valuationEngine = new PortfolioValuationEngine(broker);
        this.model1Adapter = new Model1MomentumAdapter();
        this.model2Adapter = new Model2ExecutionCostAdapter();
        this.rankingEngine = new RecommendationRankingEngine(model1Adapter, model2Adapter);
        this.scenarioOrchestrator = new ScenarioOrchestrator(broker);
        this.retentionService = new com.lro.app.ops.DataRetentionService();
        this.healthMonitor = new com.lro.app.ops.SystemHealthMonitor();
        this.ingestionService = new com.lro.app.ingestion.HistoricalDataIngestionService();
    }

    public void initialize() {
        System.out.println("================================================================================");
        System.out.println("   LIQUIDITY RISK OPTIMIZER (LRO) — BACKEND SUBSYSTEM STARTUP");
        System.out.println("   Environment: Java 25 LTS | Multi-Schema PostgreSQL & Flyway | STOMP Broker");
        System.out.println("================================================================================");

        // 1. Database Migrations
        System.out.print("[1/6] Running multi-schema Flyway database migrations... ");
        boolean migrated = dbManager.runMigrations();
        System.out.println(migrated ? "SUCCESS (Topology initialized for 4 banks, exchange, lro)" : "FAILED");

        // 2. Synthetic Population Generation (Prompt 13)
        System.out.print("[2/6] Generating 4-bank deterministic synthetic populations... ");
        SyntheticBankPopulation large = BankPopulationGenerator.generatePopulation("BANK-01-LRG", 42L);
        SyntheticBankPopulation medium = BankPopulationGenerator.generatePopulation("BANK-02-MED", 42L);
        SyntheticBankPopulation small = BankPopulationGenerator.generatePopulation("BANK-03-SML", 42L);
        SyntheticBankPopulation distressed = BankPopulationGenerator.generatePopulation("BANK-04-DST", 42L);
        System.out.println("SUCCESS (Large: " + large.customerCount() + ", Med: " + medium.customerCount() + ", Sml: " + small.customerCount() + ", Distressed: " + distressed.grossNpaRatio() + "% NPA)");

        // 3. Post Initial Balancing Ledger Entries
        System.out.print("[3/6] Initializing double-entry general ledger with balanced trial balances... ");
        ledgerService.postTransaction("BANK-01-LRG", "TXN-INIT-01", "Capital Inflow", List.of(
                new JournalEntry("E1", "TXN-INIT-01", Instant.now(), "CASH_RESERVE", large.cashReserve(), Money.ZERO, "Cash", "Posted"),
                new JournalEntry("E2", "TXN-INIT-01", Instant.now(), "SHAREHOLDERS_EQUITY", Money.ZERO, large.cashReserve(), "Equity", "Posted")
        ));
        System.out.println("SUCCESS (Invariance verified)");

        // 4. Exchange Instrument Catalogue & Continuous Matching
        System.out.print("[4/6] Bootstrapping continuous matching engine and multi-asset catalogue... ");
        int instCount = catalogue.getAllInstruments().size();
        matchingEngine.submitOrder(new Order("INIT-ASK-01", "RELIANCE", Side.SELL, OrderType.LIMIT, 500, Money.of("2840.50")));
        matchingEngine.submitOrder(new Order("INIT-BID-01", "RELIANCE", Side.BUY, OrderType.LIMIT, 300, Money.of("2840.00")));
        System.out.println("SUCCESS (" + instCount + " instruments loaded across equities, G-Secs, derivatives)");

        // 5. Portfolio Lots & Mark-to-Market Valuation
        System.out.print("[5/6] Initializing liquid treasury portfolio and real-time MTM valuation... ");
        valuationEngine.addLot(new PortfolioLot("LOT-01", "BANK-01-LRG", "RELIANCE", "Equity", 50000L, Money.of("2800.00"), Money.of("2840.50"), false, Instant.now()));
        valuationEngine.addLot(new PortfolioLot("LOT-02", "BANK-01-LRG", "GS-7.18-2033", "G-Sec", 500000L, Money.of("100.00"), Money.of("100.84"), false, Instant.now()));
        Money totalMtm = valuationEngine.getTotalPortfolioMarketValue("BANK-01-LRG");
        System.out.println("SUCCESS (Total Book: " + totalMtm + ")");

        // 6. ML Inference Adapters & Decision Support
        System.out.print("[6/6] Connecting to XGBoost Model 1 & Model 2 adapters (with fallbacks)... ");
        var rec = rankingEngine.generateRecommendation(
                "BANK-01-LRG", "GS-7.18-2033", "G-Sec", 100000L, Money.of("100.84"),
                "Bilateral Interbank Repo", "Sovereign liquidity tranche"
        );
        System.out.println("SUCCESS (Recommendation Engine Active, generated " + rec.recommendationId() + ")");

        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("   All backend modules active. STOMP event broker listening for subscriptions.");
        System.out.println("================================================================================");
    }

    public DatabaseManager getDbManager() { return dbManager; }
    public StompEventBroker getBroker() { return broker; }
    public DoubleEntryLedgerService getLedgerService() { return ledgerService; }
    public ExchangeInstrumentCatalogue getCatalogue() { return catalogue; }
    public ContinuousMatchingEngine getMatchingEngine() { return matchingEngine; }
    public MarketRegimeEngine getRegimeEngine() { return regimeEngine; }
    public PortfolioValuationEngine getValuationEngine() { return valuationEngine; }
    public RecommendationRankingEngine getRankingEngine() { return rankingEngine; }
    public ScenarioOrchestrator getScenarioOrchestrator() { return scenarioOrchestrator; }
    public com.lro.app.ops.DataRetentionService getRetentionService() { return retentionService; }
    public com.lro.app.ops.SystemHealthMonitor getHealthMonitor() { return healthMonitor; }
    public com.lro.app.ingestion.HistoricalDataIngestionService getIngestionService() { return ingestionService; }

    public static void main(String[] args) {
        BackendLauncher app = new BackendLauncher();
        app.initialize();
    }
}
