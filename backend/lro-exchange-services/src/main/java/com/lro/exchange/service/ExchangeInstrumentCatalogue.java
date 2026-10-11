package com.lro.exchange.service;

import com.lro.common.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exchange Instrument Catalogue & Population Generator implementing Prompt 16.
 * Covers Indian market families with tick/lot rules and metadata.
 */
public class ExchangeInstrumentCatalogue {

    public record Instrument(
            String symbol,
            String isin,
            String name,
            String category, // "Equity", "Index", "ETF", "G-Sec", "T-Bill", "Corporate Bond", "Futures", "Options", "FX", "Commodity"
            String sector,
            BigDecimal tickSize,
            int lotSize,
            Money lastTradedPrice,
            Money bidPrice,
            Money askPrice,
            long dayVolume,
            Money dayTurnover,
            BigDecimal couponOrYieldPct,
            LocalDate maturityOrExpiry,
            BigDecimal strikePrice,
            String optionType
    ) {
        public String currency() {
            return "INR";
        }

        public String issuer() {
            return "G-Sec".equalsIgnoreCase(category) ? "GOVERNMENT_OF_INDIA" : name;
        }
    }

    private final Map<String, Instrument> catalogue = new ConcurrentHashMap<>();

    public ExchangeInstrumentCatalogue() {
        seedCatalogue();
    }

    public Optional<Instrument> findInstrument(String symbol) {
        return getInstrument(symbol);
    }

    private void seedCatalogue() {
        LocalDate today = LocalDate.now();

        // 1. Equities
        add(new Instrument("RELIANCE", "INE002A01018-SIM", "Reliance Industries (Simulated)", "Equity", "Energy", new BigDecimal("0.05"), 1, Money.of("2840.50"), Money.of("2840.25"), Money.of("2840.75"), 2100000L, Money.of("5965000000.00"), null, null, null, null));
        add(new Instrument("HDFCBANK", "INE040A01034-SIM", "HDFC Bank Ltd (Simulated)", "Equity", "Banking", new BigDecimal("0.05"), 1, Money.of("1685.20"), Money.of("1685.00"), Money.of("1685.50"), 3400000L, Money.of("5729000000.00"), null, null, null, null));
        add(new Instrument("INFY", "INE009A01021-SIM", "Infosys Ltd (Simulated)", "Equity", "IT", new BigDecimal("0.05"), 1, Money.of("1820.75"), Money.of("1820.50"), Money.of("1821.00"), 1850000L, Money.of("3368000000.00"), null, null, null, null));
        add(new Instrument("TCS", "INE467B01029-SIM", "Tata Consultancy Services (Simulated)", "Equity", "IT", new BigDecimal("0.05"), 1, Money.of("4150.00"), Money.of("4149.50"), Money.of("4150.50"), 920000L, Money.of("3818000000.00"), null, null, null, null));
        add(new Instrument("ICICIBANK", "INE090A01021-SIM", "ICICI Bank Ltd (Simulated)", "Equity", "Banking", new BigDecimal("0.05"), 1, Money.of("1210.40"), Money.of("1210.10"), Money.of("1210.60"), 4100000L, Money.of("4962000000.00"), null, null, null, null));

        // 2. Sovereign G-Secs & T-Bills
        add(new Instrument("GS-7.18-2033", "IN0020230085-SIM", "7.18% GS 2033 Benchmark", "G-Sec", "Sovereign Debt", new BigDecimal("0.01"), 10000, Money.of("100.84"), Money.of("100.82"), Money.of("100.86"), 15000000L, Money.of("1512600000.00"), new BigDecimal("7.18"), today.plusYears(9), null, null));
        add(new Instrument("GS-7.06-2028", "IN0020180025-SIM", "7.06% GS 2028 Benchmark", "G-Sec", "Sovereign Debt", new BigDecimal("0.01"), 10000, Money.of("100.50"), Money.of("100.48"), Money.of("100.52"), 8500000L, Money.of("854250000.00"), new BigDecimal("7.06"), today.plusYears(4), null, null));
        add(new Instrument("TBILL-91D", "IN0091TB2401-SIM", "91-Day Treasury Bill", "T-Bill", "Money Market", new BigDecimal("0.01"), 10000, Money.of("98.32"), Money.of("98.31"), Money.of("98.33"), 22000000L, Money.of("2163040000.00"), new BigDecimal("6.85"), today.plusDays(45), null, null));

        // 3. Corporate Bonds
        add(new Instrument("HDFCB-8.15-29", "INE040A08427-SIM", "8.15% HDFC Bank AAA Bond 2029", "Corporate Bond", "Financials", new BigDecimal("0.05"), 100, Money.of("998.00"), Money.of("997.50"), Money.of("998.50"), 350000L, Money.of("349300000.00"), new BigDecimal("8.15"), today.plusYears(5), null, null));

        // 4. Derivatives (Futures & Options)
        add(new Instrument("NIFTY-FUT-1M", "FUT-NIFTY-26OCT-SIM", "Nifty 50 Futures 1M", "Futures", "Index Derivatives", new BigDecimal("0.05"), 25, Money.of("24910.00"), Money.of("24909.50"), Money.of("24911.00"), 520000L, Money.of("12953200000.00"), null, today.plusDays(20), null, null));
        add(new Instrument("NIFTY-25000-CE", "OPT-NIFTY-25000CE-SIM", "Nifty 25000 Call Option", "Options", "Index Derivatives", new BigDecimal("0.05"), 25, Money.of("145.20"), Money.of("145.00"), Money.of("145.50"), 2400000L, Money.of("348480000.00"), null, today.plusDays(20), new BigDecimal("25000.00"), "CE"));

        // 5. FX & Commodities
        add(new Instrument("USDINR-SPOT", "FX-USDINR-SIM", "USD / INR Spot Simulated", "FX", "Currency", new BigDecimal("0.0025"), 1000, Money.of("83.9850"), Money.of("83.9825"), Money.of("83.9875"), 9200000L, Money.of("772662000.00"), null, null, null, null));
        add(new Instrument("GOLD-10G", "COM-GOLD-10G-SIM", "Gold 999 10g Simulated", "Commodity", "Precious Metals", new BigDecimal("1.00"), 1, Money.of("75800.00"), Money.of("75790.00"), Money.of("75810.00"), 78000L, Money.of("5912400000.00"), null, null, null, null));
    }

    private void add(Instrument inst) {
        catalogue.put(inst.symbol().toUpperCase(), inst);
    }

    public Optional<Instrument> getInstrument(String symbol) {
        if (symbol == null) return Optional.empty();
        return Optional.ofNullable(catalogue.get(symbol.toUpperCase()));
    }

    public List<Instrument> getAllInstruments() {
        return new ArrayList<>(catalogue.values());
    }

    public List<Instrument> getByCategory(String category) {
        return catalogue.values().stream()
                .filter(i -> i.category().equalsIgnoreCase(category))
                .toList();
    }
}
