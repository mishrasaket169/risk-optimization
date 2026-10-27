package com.lro.frontend;

import com.lro.frontend.bank.model.BankModels.*;
import com.lro.frontend.bank.service.MockBankService;
import com.lro.frontend.common.Formatters;
import com.lro.frontend.exchange.model.ExchangeModels.*;
import com.lro.frontend.exchange.service.MockExchangeService;
import com.lro.frontend.lro.model.LroModels.*;
import com.lro.frontend.lro.service.MockLroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FrontendUnitTests {

    private MockBankService bankService;
    private MockExchangeService exchangeService;
    private MockLroService lroService;

    @BeforeEach
    void setUp() {
        bankService = new MockBankService();
        exchangeService = new MockExchangeService();
        lroService = new MockLroService();
    }

    @Test
    @DisplayName("Verify Prompt 01 & 02: 4 Banks exist with distinct populations & NPA profiles")
    void testFourBankProfilesExist() {
        List<BankProfile> profiles = bankService.getAllBankProfiles();
        assertEquals(4, profiles.size(), "Must have exactly 4 fictional bank profiles");

        Optional<BankProfile> largeBank = bankService.getBankProfile("BANK-01-LRG");
        assertTrue(largeBank.isPresent());
        assertTrue(largeBank.get().customerCount() >= 10000, "Large bank must have >= 10,000 customers");
        assertEquals("Healthy", largeBank.get().healthStatus());
        assertTrue(largeBank.get().grossNpaRatio().compareTo(new BigDecimal("2.0")) < 0);

        Optional<BankProfile> distressedBank = bankService.getBankProfile("BANK-04-DST");
        assertTrue(distressedBank.isPresent());
        assertEquals("Distressed", distressedBank.get().healthStatus());
        assertTrue(distressedBank.get().grossNpaRatio().compareTo(new BigDecimal("10.0")) > 0, "Distressed bank must have > 10% NPA");
    }

    @Test
    @DisplayName("Verify Prompt 02: Bank switching isolates customer data without row leakage")
    void testBankDataIsolation() {
        List<CustomerRecord> largeCusts = bankService.getCustomers("BANK-01-LRG", "", "All");
        List<CustomerRecord> distressedCusts = bankService.getCustomers("BANK-04-DST", "", "All");

        assertFalse(largeCusts.isEmpty());
        assertFalse(distressedCusts.isEmpty());

        for (CustomerRecord c : largeCusts) {
            assertTrue(c.customerId().startsWith("BANK-01-LRG"));
        }
        for (CustomerRecord c : distressedCusts) {
            assertTrue(c.customerId().startsWith("BANK-04-DST"));
        }
    }

    @Test
    @DisplayName("Verify Prompt 03 & 04: Formatting precision with explicit INR & Crores notation")
    void testMonetaryFormatting() {
        BigDecimal val1 = new BigDecimal("1234567.89");
        assertEquals("₹ 12,34,567.89", Formatters.formatINR(val1));

        BigDecimal valCr = new BigDecimal("5000000000.00"); // 500 Cr
        assertEquals("₹ 500.00 Cr", Formatters.formatINRCrores(valCr));

        BigDecimal bps = new BigDecimal("45.2");
        assertEquals("45.2 bps", Formatters.formatBps(bps));
    }

    @Test
    @DisplayName("Verify Prompt 05 & 06: Exchange instruments coverage across asset classes")
    void testExchangeInstrumentsAndOrderTicket() {
        List<InstrumentRecord> equities = exchangeService.getInstruments("Equity", "");
        assertFalse(equities.isEmpty(), "Must contain equities");

        List<InstrumentRecord> gsecs = exchangeService.getInstruments("G-Sec", "");
        assertFalse(gsecs.isEmpty(), "Must contain sovereign G-Secs");

        List<InstrumentRecord> derivatives = exchangeService.getInstruments("Futures", "");
        assertFalse(derivatives.isEmpty(), "Must contain derivatives");

        // Submit simulated order
        OrderRecord order = exchangeService.submitSimulatedOrder("RELIANCE", "BUY", "LIMIT", 50, new BigDecimal("2840.00"), "DAY");
        assertNotNull(order);
        assertEquals("Filled", order.status());
        assertEquals(50, order.filledQuantity());

        // Check trade blotter
        List<TradeRecord> trades = exchangeService.getTradeBlotter();
        assertFalse(trades.isEmpty());
        assertEquals("RELIANCE", trades.get(0).symbol());
    }

    @Test
    @DisplayName("Verify Prompt 07 & 08: LRO decision support, recommendations & human approval workflow")
    void testLroDecisionSupportAndApprovals() {
        LiquiditySnapshot snapLarge = lroService.getSnapshot("BANK-01-LRG");
        assertTrue(snapLarge.educationalLcrPercent().compareTo(new BigDecimal("100.0")) > 0, "Healthy bank LCR > 100%");

        LiquiditySnapshot snapDistressed = lroService.getSnapshot("BANK-04-DST");
        assertTrue(snapDistressed.educationalLcrPercent().compareTo(new BigDecimal("100.0")) < 0, "Distressed bank breaches 100% LCR");

        // Recommendations
        List<RecommendationItem> recs = lroService.getRecommendations("BANK-01-LRG");
        assertFalse(recs.isEmpty());
        RecommendationItem firstRec = recs.get(0);
        assertEquals("Pending Review", firstRec.status());

        // Officer approval
        boolean approved = lroService.approveRecommendation(firstRec.recommendationId(), "Checked treasury funding schedule");
        assertTrue(approved);

        List<RecommendationItem> updatedRecs = lroService.getRecommendations("BANK-01-LRG");
        assertEquals("Approved by Treasury Officer", updatedRecs.get(0).status());
    }

    @Test
    @DisplayName("Verify Prompt 08: Model insights registry transparency")
    void testModelInsightsRegistry() {
        List<ModelInsight> insights = lroService.getModelInsights();
        assertEquals(2, insights.size(), "Must document Model 1 and Model 2");

        ModelInsight m1 = insights.stream().filter(m -> m.modelId().equals("MODEL-01-MOMENTUM")).findFirst().orElseThrow();
        assertEquals("T+5 Trading Days", m1.forecastHorizon());
        assertFalse(m1.limitationsNotice().isBlank());

        ModelInsight m2 = insights.stream().filter(m -> m.modelId().equals("MODEL-02-EXEC-COST")).findFirst().orElseThrow();
        assertTrue(m2.targetDefinition().contains("slippage"));
    }

    @Test
    @DisplayName("Verify Stock, Bond, and Securities tables each have >= 1,000 entries and canonical entries")
    void testStockBondSecuritiesDirectoryCoverage() {
        List<StockRecord> stocks = exchangeService.getStocks("All", "");
        assertTrue(stocks.size() >= 1000, "Must have >= 1,000 stocks, found: " + stocks.size());
        StockRecord reliance = stocks.stream().filter(s -> s.symbol().equals("RELIANCE")).findFirst().orElseThrow();
        assertEquals("Reliance Industries Limited", reliance.companyName());
        assertEquals("Energy / Conglomerate", reliance.sector());

        List<BondRecord> bonds = exchangeService.getBonds("All", "");
        assertTrue(bonds.size() >= 1000, "Must have >= 1,000 bonds, found: " + bonds.size());
        BondRecord hdfcBond = bonds.stream().filter(b -> b.symbol().equals("HDFCB-8.15-29")).findFirst().orElseThrow();
        assertEquals("8.15% HDFC Bank AAA Bond 2029", hdfcBond.issuerSeries());
        assertEquals("Financial Services", hdfcBond.sectorType());

        List<SecurityRecord> securities = exchangeService.getSecurities("All", "");
        assertTrue(securities.size() >= 1000, "Must have >= 1,000 securities, found: " + securities.size());
        SecurityRecord gsec = securities.stream().filter(s -> s.symbol().equals("GS-7.18-2033")).findFirst().orElseThrow();
        assertEquals("7.18% Government of India Sovereign 2033", gsec.securityDescription());
        assertEquals("G-Sec", gsec.category());
    }

    @Test
    @DisplayName("Verify Balance Sheet table, 2-year history scale, and accounting invariance across 4 banks")
    void testBalanceSheetAcrossFourBanks() {
        String[] bankIds = {"BANK-01-LRG", "BANK-02-MED", "BANK-03-SML", "BANK-04-DST"};
        long[] expectedTxns = {1000000L, 500000L, 300000L, 180000L};

        for (int i = 0; i < bankIds.length; i++) {
            String bId = bankIds[i];
            BalanceSheetSummary summary = bankService.getBalanceSheetSummary(bId);
            assertNotNull(summary);
            assertTrue(summary.totalAssets().compareTo(BigDecimal.ZERO) > 0, "Total assets must be positive for " + bId);
            assertTrue(summary.totalLiabilities().compareTo(BigDecimal.ZERO) > 0, "Total liabilities must be positive for " + bId);
            assertTrue(summary.totalEquity().compareTo(BigDecimal.ZERO) > 0, "Total equity must be positive for " + bId);
            assertTrue(summary.isBalanced(), "Balance sheet must be balanced (Assets = Liabilities + Equity) for " + bId);
            assertEquals(expectedTxns[i], summary.totalHistoricalTransactions(), "Historical transaction scale must match expected for " + bId);

            List<BalanceSheetRecord> records = bankService.getBalanceSheetRecords(bId);
            assertFalse(records.isEmpty(), "Balance sheet records must not be empty for " + bId);
            for (BalanceSheetRecord r : records) {
                assertNotNull(r.recordId());
                assertNotNull(r.lineItem());
                assertNotNull(r.category());
                assertTrue(r.category().equals("Asset") || r.category().equals("Liability") || r.category().equals("Equity"));
            }
        }
    }

    @Test
    @DisplayName("Verify Exchange Simulator Lifecycle & Start/Pause/Resume")
    void testExchangeSimulatorLifecycle() throws Exception {
        assertNotNull(exchangeService.getSimulator());
        assertFalse(exchangeService.isSimulationRunning(), "Simulator should initially be idle");

        exchangeService.startSimulation();
        assertTrue(exchangeService.isSimulationRunning(), "Simulator must be running after startSimulation()");

        // Wait a short moment for ticks to process
        Thread.sleep(150);
        assertTrue(exchangeService.getSimulator().getTickCount() > 0, "Ticks must increment while simulation is active");

        exchangeService.pauseSimulation();
        assertFalse(exchangeService.isSimulationRunning(), "Simulator must be paused after pauseSimulation()");

        long ticksPaused = exchangeService.getSimulator().getTickCount();
        Thread.sleep(100);
        assertEquals(ticksPaused, exchangeService.getSimulator().getTickCount(), "Ticks must not increment while paused");

        exchangeService.resumeSimulation();
        assertTrue(exchangeService.isSimulationRunning(), "Simulator must be running after resumeSimulation()");
        exchangeService.pauseSimulation();
    }

    @Test
    @DisplayName("Verify Stock Values Fluctuate According to Defined Market Regimes")
    void testStockFluctuationAccordingToMarketRegimes() {
        var sim = exchangeService.getSimulator();
        assertNotNull(sim);

        // 1. Test BULL Regime: strong buying momentum, green gains dominate (>70% advances)
        exchangeService.updateMarketRegime("Bull", 1, 42L);
        assertEquals("Bull", exchangeService.getCurrentRegime());
        assertEquals(com.lro.frontend.exchange.service.ExchangeMarketSimulator.MarketRegime.BULL, sim.getRegime());
        assertEquals(new BigDecimal("0.85"), sim.getSpreadMultiplier());

        MarketOverview bullOverview = exchangeService.getMarketOverview();
        assertTrue(bullOverview.advancesCount() > bullOverview.declinesCount(), "Bull regime must have more advances than declines");
        double bullAdvRatio = (double) bullOverview.advancesCount() / (bullOverview.advancesCount() + bullOverview.declinesCount());
        assertTrue(bullAdvRatio >= 0.70, "Bull regime must have >= 70% advancing stocks, found: " + bullAdvRatio);

        // Run discrete ticks in Bull regime
        sim.step(15);
        assertTrue(sim.getTickCount() >= 15);
        assertTrue(sim.getTradesCount() >= 15);

        // 2. Test BEAR Regime: selling pressure, red decliners dominate (>70% declines)
        exchangeService.updateMarketRegime("Bear", 1, 42L);
        assertEquals("Bear", exchangeService.getCurrentRegime());
        assertEquals(com.lro.frontend.exchange.service.ExchangeMarketSimulator.MarketRegime.BEAR, sim.getRegime());
        assertEquals(new BigDecimal("1.60"), sim.getSpreadMultiplier());

        MarketOverview bearOverview = exchangeService.getMarketOverview();
        assertTrue(bearOverview.declinesCount() > bearOverview.advancesCount(), "Bear regime must have more declines than advances");
        double bearDecRatio = (double) bearOverview.declinesCount() / (bearOverview.advancesCount() + bearOverview.declinesCount());
        assertTrue(bearDecRatio >= 0.70, "Bear regime must have >= 70% declining stocks, found: " + bearDecRatio);

        sim.step(15);

        // 3. Test CRASH Regime: panic liquidation, blown out spreads (3.80x), overwhelming decliners (>90%)
        exchangeService.updateMarketRegime("Crash", 1, 42L);
        assertEquals("Crash", exchangeService.getCurrentRegime());
        assertEquals(com.lro.frontend.exchange.service.ExchangeMarketSimulator.MarketRegime.CRASH, sim.getRegime());
        assertEquals(new BigDecimal("3.80"), sim.getSpreadMultiplier());

        MarketOverview crashOverview = exchangeService.getMarketOverview();
        double crashDecRatio = (double) crashOverview.declinesCount() / (crashOverview.advancesCount() + crashOverview.declinesCount());
        assertTrue(crashDecRatio >= 0.90, "Crash regime must have >= 90% declining stocks under panic selling, found: " + crashDecRatio);

        sim.step(15);

        // 4. Test NORMAL Regime: balanced continuous auction
        exchangeService.updateMarketRegime("Normal", 1, 42L);
        assertEquals("Normal", exchangeService.getCurrentRegime());
        assertEquals(com.lro.frontend.exchange.service.ExchangeMarketSimulator.MarketRegime.NORMAL, sim.getRegime());
        assertEquals(new BigDecimal("1.00"), sim.getSpreadMultiplier());

        MarketOverview normOverview = exchangeService.getMarketOverview();
        double normAdvRatio = (double) normOverview.advancesCount() / (normOverview.advancesCount() + normOverview.declinesCount());
        assertTrue(normAdvRatio >= 0.35 && normAdvRatio <= 0.65, "Normal regime must have balanced advances/declines, found: " + normAdvRatio);
    }
}
