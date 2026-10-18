package com.lro.frontend.exchange.service;

import com.lro.frontend.exchange.model.ExchangeModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MockExchangeService implements ExchangeService {

    private String currentRegime = "Normal";
    private int simulationSpeed = 1;
    private long currentSeed = 42L;

    public int getSimulationSpeed() {
        return simulationSpeed;
    }

    public long getCurrentSeed() {
        return currentSeed;
    }

    private final List<MarketIndex> indices = new CopyOnWriteArrayList<>();
    private final List<StockRecord> stocks = new CopyOnWriteArrayList<>();
    private final List<BondRecord> bonds = new CopyOnWriteArrayList<>();
    private final List<SecurityRecord> securities = new CopyOnWriteArrayList<>();
    private final List<InstrumentRecord> instruments = new CopyOnWriteArrayList<>();
    private final List<OrderRecord> orders = new CopyOnWriteArrayList<>();
    private final List<TradeRecord> trades = new CopyOnWriteArrayList<>();
    private final List<String> marketEvents = new CopyOnWriteArrayList<>();

    private final Map<String, Integer> stockIndices = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> bondIndices = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> securityIndices = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> instrumentIndices = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, BigDecimal> basePrices = new java.util.concurrent.ConcurrentHashMap<>();

    private final ExchangeMarketSimulator simulator;

    public MockExchangeService() {
        this(null);
    }

    public MockExchangeService(javax.sql.DataSource dataSource) {
        seedExchangeData();
        this.simulator = new ExchangeMarketSimulator(this, dataSource);
    }

    private void seedExchangeData() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        indices.add(new MarketIndex("SIM-NIFTY50", "Simulated Nifty 50 Index", new BigDecimal("24850.40"), new BigDecimal("0.65"), new BigDecimal("24920.00"), new BigDecimal("24780.10")));
        indices.add(new MarketIndex("SIM-BANKNIFTY", "Simulated Bank Nifty Index", new BigDecimal("51420.80"), new BigDecimal("1.12"), new BigDecimal("51600.00"), new BigDecimal("51100.50")));
        indices.add(new MarketIndex("SIM-MIDCAP", "Simulated Nifty Midcap 100", new BigDecimal("56200.15"), new BigDecimal("-0.24"), new BigDecimal("56450.00"), new BigDecimal("55980.00")));

        // Attempt live load from PostgreSQL first
        boolean loadedFromDb = loadFromPostgresIfAvailable();

        // If PostgreSQL is not available or has fewer than 1000 records, populate via synthetic generation
        if (!loadedFromDb || stocks.size() < 1000 || bonds.size() < 1000 || securities.size() < 1000) {
            stocks.clear();
            bonds.clear();
            securities.clear();
            seedInMemoryStocks();
            seedInMemoryBonds();
            seedInMemorySecurities();
        }

        // Build comprehensive instruments list for compatibility with order tickets, order book depth, etc.
        instruments.clear();
        for (StockRecord s : stocks) {
            instruments.add(new InstrumentRecord(
                    "INE-" + s.symbol() + "-SIM",
                    s.symbol(),
                    s.companyName(),
                    "Equity",
                    s.sector(),
                    new BigDecimal("0.05"),
                    1,
                    s.lastTradedPrice(),
                    s.changePercent(),
                    s.bidPrice(),
                    s.askPrice(),
                    s.dayVolume(),
                    s.dayTurnoverINR(),
                    now,
                    null,
                    null,
                    null,
                    null
            ));
        }
        for (BondRecord b : bonds) {
            instruments.add(new InstrumentRecord(
                    "INE-" + b.symbol() + "-SIM",
                    b.symbol(),
                    b.issuerSeries(),
                    "Corporate Bond",
                    b.sectorType(),
                    new BigDecimal("0.05"),
                    100,
                    b.cleanPrice(),
                    b.changePercent(),
                    b.bidPrice(),
                    b.askPrice(),
                    b.dayVolume(),
                    b.cleanPrice().multiply(BigDecimal.valueOf(b.dayVolume())).setScale(2, RoundingMode.HALF_UP),
                    now,
                    b.couponRatePercent(),
                    b.maturityDate(),
                    null,
                    null
            ));
        }
        for (SecurityRecord sc : securities) {
            instruments.add(new InstrumentRecord(
                    "IN-" + sc.symbol() + "-SIM",
                    sc.symbol(),
                    sc.securityDescription(),
                    sc.category(),
                    sc.category().equalsIgnoreCase("T-Bill") ? "Money Market" : "Sovereign Debt",
                    new BigDecimal("0.01"),
                    10000,
                    sc.cleanPrice(),
                    sc.changePercent(),
                    sc.bidPrice(),
                    sc.askPrice(),
                    sc.dayVolume(),
                    sc.cleanPrice().multiply(BigDecimal.valueOf(sc.dayVolume())).setScale(2, RoundingMode.HALF_UP),
                    now,
                    sc.benchmarkYieldPercent(),
                    sc.maturityDate(),
                    null,
                    null
            ));
        }

        // Additional derivatives, FX and Commodity instruments
        instruments.add(new InstrumentRecord("FUT-NIFTY-26OCT-SIM", "NIFTY-FUT-1M", "Nifty Index Future Oct 2026", "Futures", "Index Derivatives", new BigDecimal("0.05"), 25, new BigDecimal("24910.00"), new BigDecimal("0.70"), new BigDecimal("24909.50"), new BigDecimal("24911.00"), 340000L, new BigDecimal("8469000000.00"), now, null, today.plusDays(20), null, null));
        instruments.add(new InstrumentRecord("OPT-NIFTY-25000CE-SIM", "NIFTY-25000-CE", "Nifty 25000 Call Option", "Options", "Index Derivatives", new BigDecimal("0.05"), 25, new BigDecimal("145.20"), new BigDecimal("8.50"), new BigDecimal("145.00"), new BigDecimal("145.50"), 1200000L, new BigDecimal("174240000.00"), now, null, today.plusDays(20), new BigDecimal("25000.00"), "CE"));
        instruments.add(new InstrumentRecord("FX-USDINR-SIM", "USDINR-SPOT", "US Dollar / Indian Rupee", "FX", "Currency", new BigDecimal("0.0025"), 1000, new BigDecimal("83.9850"), new BigDecimal("0.05"), new BigDecimal("83.9825"), new BigDecimal("83.9875"), 5500000L, new BigDecimal("461917500.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("COM-GOLD-10G-SIM", "GOLD-10G", "Gold 999 10 Grams (Simulated)", "Commodity", "Precious Metals", new BigDecimal("1.00"), 1, new BigDecimal("75800.00"), new BigDecimal("0.40"), new BigDecimal("75790.00"), new BigDecimal("75810.00"), 42000L, new BigDecimal("318360000.00"), now, null, null, null, null));

        // Seed Orders and Trades
        orders.add(new OrderRecord("ORD-1001", "CL-DEMO-01", now.minusSeconds(1200), "RELIANCE", "BUY", "LIMIT", 500, 500, new BigDecimal("2840.00"), new BigDecimal("2840.00"), "Filled", "DAY", new BigDecimal("1420000.00")));
        orders.add(new OrderRecord("ORD-1002", "CL-DEMO-02", now.minusSeconds(800), "HDFCBANK", "SELL", "LIMIT", 1000, 400, new BigDecimal("1686.00"), new BigDecimal("1686.00"), "Partially Filled", "DAY", new BigDecimal("1686000.00")));
        orders.add(new OrderRecord("ORD-1003", "CL-DEMO-03", now.minusSeconds(300), "GS-7.18-2033", "BUY", "LIMIT", 50000, 0, new BigDecimal("100.80"), BigDecimal.ZERO, "Accepted", "DAY", new BigDecimal("5040000.00")));

        trades.add(new TradeRecord("TRD-9001", "ORD-1001", now.minusSeconds(1190), "RELIANCE", "BUY", 500, new BigDecimal("2840.00"), new BigDecimal("1420000.00")));
        trades.add(new TradeRecord("TRD-9002", "ORD-1002", now.minusSeconds(790), "HDFCBANK", "SELL", 400, new BigDecimal("1686.00"), new BigDecimal("674400.00")));

        marketEvents.add("Simulated Morning Auction completed cleanly.");
        marketEvents.add("RBI Monetary Policy Stance simulated in Normal regime.");
        marketEvents.add("Sovereign G-Sec matching active with tight bid-ask spreads.");
        rebuildLookupIndices();
    }

    private boolean loadFromPostgresIfAvailable() {
        String url = System.getenv().getOrDefault("LRO_DB_URL", "jdbc:postgresql://localhost:5432/lro_db");
        String user = System.getenv().getOrDefault("LRO_DB_USER", "postgres");
        String pass = System.getenv().getOrDefault("LRO_DB_PASSWORD", "Saket@169");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // Load stocks
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT symbol, company_name, sector, ltp, change_pct, bid_price, ask_price, volume, turnover FROM exchange.stocks ORDER BY CASE WHEN symbol IN ('RELIANCE','HDFCBANK','INFY','TCS','ICICIBANK','SBIN','BHARTIARTL','LT','TATAMOTORS','ITC') THEN 0 ELSE 1 END, symbol")) {
                while (rs.next()) {
                    stocks.add(new StockRecord(
                            rs.getString("symbol"),
                            rs.getString("company_name"),
                            rs.getString("sector"),
                            rs.getBigDecimal("ltp"),
                            rs.getBigDecimal("change_pct"),
                            rs.getBigDecimal("bid_price"),
                            rs.getBigDecimal("ask_price"),
                            rs.getLong("volume"),
                            rs.getBigDecimal("turnover")
                    ));
                }
            }

            // Load bonds
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT bond_symbol, issuer_series, sector_type, coupon_rate_pct, clean_price, change_pct, bid, ask, volume, maturity_date FROM exchange.bonds ORDER BY CASE WHEN bond_symbol IN ('HDFCB-8.15-29','REC-7.65-28','PFC-7.75-30','REL-8.25-31','TATA-8.40-32','NHAI-7.90-27') THEN 0 ELSE 1 END, bond_symbol")) {
                while (rs.next()) {
                    bonds.add(new BondRecord(
                            rs.getString("bond_symbol"),
                            rs.getString("issuer_series"),
                            rs.getString("sector_type"),
                            rs.getBigDecimal("coupon_rate_pct"),
                            rs.getBigDecimal("clean_price"),
                            rs.getBigDecimal("change_pct"),
                            rs.getBigDecimal("bid"),
                            rs.getBigDecimal("ask"),
                            rs.getLong("volume"),
                            rs.getDate("maturity_date").toLocalDate()
                    ));
                }
            }

            // Load securities
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT security_symbol, security_description, category, benchmark_yield_pct, clean_price, change_pct, bid, ask, volume, maturity_date FROM exchange.securities ORDER BY CASE WHEN security_symbol IN ('GS-7.18-2033','GS-7.06-2028','GS-7.26-2032','GS-7.38-2027','TBILL-91D','TBILL-182D','TBILL-364D') THEN 0 ELSE 1 END, security_symbol")) {
                while (rs.next()) {
                    securities.add(new SecurityRecord(
                            rs.getString("security_symbol"),
                            rs.getString("security_description"),
                            rs.getString("category"),
                            rs.getBigDecimal("benchmark_yield_pct"),
                            rs.getBigDecimal("clean_price"),
                            rs.getBigDecimal("change_pct"),
                            rs.getBigDecimal("bid"),
                            rs.getBigDecimal("ask"),
                            rs.getLong("volume"),
                            rs.getDate("maturity_date").toLocalDate()
                    ));
                }
            }
            return !stocks.isEmpty() && !bonds.isEmpty() && !securities.isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    private void seedInMemoryStocks() {
        // Canonical 10 stocks matching screenshot
        stocks.add(new StockRecord("RELIANCE", "Reliance Industries Limited", "Energy / Conglomerate", new BigDecimal("2840.50"), new BigDecimal("1.20"), new BigDecimal("2840.25"), new BigDecimal("2840.75"), 1845000L, new BigDecimal("5240000000.00")));
        stocks.add(new StockRecord("HDFCBANK", "HDFC Bank Limited", "Banking & Financials", new BigDecimal("1685.20"), new BigDecimal("0.85"), new BigDecimal("1685.00"), new BigDecimal("1685.50"), 2450000L, new BigDecimal("4128000000.00")));
        stocks.add(new StockRecord("INFY", "Infosys Limited", "Information Technology", new BigDecimal("1820.75"), new BigDecimal("-0.45"), new BigDecimal("1820.50"), new BigDecimal("1821.00"), 1230000L, new BigDecimal("2239000000.00")));
        stocks.add(new StockRecord("TCS", "Tata Consultancy Services Ltd", "Information Technology", new BigDecimal("4150.00"), new BigDecimal("0.30"), new BigDecimal("4149.50"), new BigDecimal("4150.50"), 820000L, new BigDecimal("3403000000.00")));
        stocks.add(new StockRecord("ICICIBANK", "ICICI Bank Limited", "Banking & Financials", new BigDecimal("1210.40"), new BigDecimal("1.45"), new BigDecimal("1210.10"), new BigDecimal("1210.60"), 3120000L, new BigDecimal("3776000000.00")));
        stocks.add(new StockRecord("SBIN", "State Bank of India", "Public Sector Banking", new BigDecimal("815.60"), new BigDecimal("-1.10"), new BigDecimal("815.30"), new BigDecimal("815.80"), 4200000L, new BigDecimal("3425500000.00")));
        stocks.add(new StockRecord("BHARTIARTL", "Bharti Airtel Limited", "Telecommunications", new BigDecimal("1540.25"), new BigDecimal("-0.65"), new BigDecimal("1540.00"), new BigDecimal("1540.50"), 1650000L, new BigDecimal("2541400000.00")));
        stocks.add(new StockRecord("LT", "Larsen & Toubro Limited", "Infrastructure & Capital Goods", new BigDecimal("3560.00"), new BigDecimal("-0.20"), new BigDecimal("3559.00"), new BigDecimal("3561.00"), 920000L, new BigDecimal("3275200000.00")));
        stocks.add(new StockRecord("TATAMOTORS", "Tata Motors Limited", "Automotive", new BigDecimal("980.50"), new BigDecimal("2.15"), new BigDecimal("980.20"), new BigDecimal("980.80"), 3400000L, new BigDecimal("3333700000.00")));
        stocks.add(new StockRecord("ITC", "ITC Limited", "FMCG / Diversified", new BigDecimal("495.30"), new BigDecimal("0.40"), new BigDecimal("495.10"), new BigDecimal("495.50"), 2800000L, new BigDecimal("1386840000.00")));

        String[] names = {
                "Adani Enterprises Ltd", "Kotak Mahindra Bank", "Axis Bank Limited", "Bajaj Finance Limited", "Wipro Limited",
                "Sun Pharmaceutical Ind", "Maruti Suzuki India", "HCL Technologies Ltd", "Asian Paints Limited", "Titan Company Limited",
                "UltraTech Cement Ltd", "NTPC Limited", "Power Grid Corporation", "Oil & Natural Gas Corp", "Coal India Limited",
                "JSW Steel Limited", "Grasim Industries Ltd", "Hindalco Industries", "Tech Mahindra Limited", "Nestle India Limited",
                "Cipla Limited", "Tata Steel Limited", "Bharat Petroleum Corp", "Eicher Motors Limited", "Dr Reddys Laboratories",
                "Britannia Industries", "Divis Laboratories Ltd", "Apollo Hospitals Ent", "Hero MotoCorp Limited", "IndusInd Bank Limited",
                "Bajaj Finserv Limited", "Shree Cement Limited", "Pidilite Industries", "Havells India Limited", "Dabur India Limited",
                "Godrej Consumer Products", "Marico Limited", "Voltas Limited", "Trent Limited", "Bharat Electronics Ltd",
                "Hindustan Aeronautics", "Zomato Limited", "One97 Communications", "Avenue Supermarts Ltd", "Polycab India Limited",
                "IRCTC Limited", "Persistent Systems Ltd", "Coforge Limited", "Mphasis Limited", "LTIMindtree Limited"
        };
        String[] symbols = {
                "ADANIENT", "KOTAKBANK", "AXISBANK", "BAJFINANCE", "WIPRO",
                "SUNPHARMA", "MARUTI", "HCLTECH", "ASIANPAINT", "TITAN",
                "ULTRACEMCO", "NTPC", "POWERGRID", "ONGC", "COALINDIA",
                "JSWSTEEL", "GRASIM", "HINDALCO", "TECHM", "NESTLEIND",
                "CIPLA", "TATASTEEL", "BPCL", "EICHERMOT", "DRREDDY",
                "BRITANNIA", "DIVISLAB", "APOLLOHOSP", "HEROMOTOCO", "INDUSINDBK",
                "BAJAJFINSV", "SHREECEM", "PIDILITIND", "HAVELLS", "DABUR",
                "GODREJCP", "MARICO", "VOLTAS", "TRENT", "BEL",
                "HAL", "ZOMATO", "PAYTM", "DMART", "POLYCAB",
                "IRCTC", "PERSISTENT", "COFORGE", "MPHASIS", "LTIM"
        };
        String[] sectors = {
                "Energy / Conglomerate", "Banking & Financials", "Banking & Financials", "Banking & Financials", "Information Technology",
                "Pharmaceuticals", "Automotive", "Information Technology", "FMCG / Diversified", "Consumer Goods",
                "Infrastructure & Capital Goods", "Power & Utilities", "Power & Utilities", "Oil & Gas", "Metals & Mining",
                "Metals & Mining", "FMCG / Diversified", "Metals & Mining", "Information Technology", "FMCG / Diversified",
                "Pharmaceuticals", "Metals & Mining", "Oil & Gas", "Automotive", "Pharmaceuticals",
                "FMCG / Diversified", "Pharmaceuticals", "Healthcare", "Automotive", "Banking & Financials",
                "Banking & Financials", "Infrastructure & Capital Goods", "Chemicals", "Consumer Goods", "FMCG / Diversified",
                "FMCG / Diversified", "FMCG / Diversified", "Consumer Goods", "Retail", "Defence & Aerospace",
                "Defence & Aerospace", "Consumer Tech", "Fintech", "Retail", "Infrastructure & Capital Goods",
                "Public Sector Services", "Information Technology", "Information Technology", "Information Technology", "Information Technology"
        };

        for (int i = 1; i <= 1000; i++) {
            int idx = (i - 1) % 50;
            int series = ((i - 1) / 50) + 1;
            String sym = series == 1 ? symbols[idx] : symbols[idx] + "-S" + series;
            String name = series == 1 ? names[idx] : names[idx] + " Series " + series;
            String sec = sectors[idx];
            BigDecimal ltp = BigDecimal.valueOf(120.0 + (i * 23.47) % 4600.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal chg = BigDecimal.valueOf(((i * 17) % 700 - 320) / 100.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bid = ltp.subtract(new BigDecimal("0.25"));
            BigDecimal ask = ltp.add(new BigDecimal("0.25"));
            long vol = 45000L + ((i * 14200L) % 5200000L);
            BigDecimal turn = ltp.multiply(BigDecimal.valueOf(vol)).setScale(2, RoundingMode.HALF_UP);

            stocks.add(new StockRecord(sym, name, sec, ltp, chg, bid, ask, vol, turn));
        }
    }

    private void seedInMemoryBonds() {
        // Canonical 6 bonds matching screenshot
        bonds.add(new BondRecord("HDFCB-8.15-29", "8.15% HDFC Bank AAA Bond 2029", "Financial Services", new BigDecimal("8.15"), new BigDecimal("998.00"), new BigDecimal("-0.10"), new BigDecimal("997.50"), new BigDecimal("998.50"), 150000L, LocalDate.of(2031, 10, 14)));
        bonds.add(new BondRecord("REC-7.65-28", "7.65% Rural Electrification Corp AAA 2028", "PSU / Infrastructure", new BigDecimal("7.65"), new BigDecimal("1002.50"), new BigDecimal("0.05"), new BigDecimal("1002.00"), new BigDecimal("1003.00"), 210000L, LocalDate.of(2030, 10, 14)));
        bonds.add(new BondRecord("PFC-7.75-30", "7.75% Power Finance Corporation AAA 2030", "Power & Utilities", new BigDecimal("7.75"), new BigDecimal("1001.20"), new BigDecimal("0.08"), new BigDecimal("1000.80"), new BigDecimal("1001.50"), 180000L, LocalDate.of(2032, 10, 14)));
        bonds.add(new BondRecord("REL-8.25-31", "8.25% Reliance Industries Debenture 2031", "Energy & Petrochem", new BigDecimal("8.25"), new BigDecimal("1005.00"), new BigDecimal("0.12"), new BigDecimal("1004.50"), new BigDecimal("1005.50"), 140000L, LocalDate.of(2033, 10, 14)));
        bonds.add(new BondRecord("TATA-8.40-32", "8.40% Tata Steel Secured Debenture 2032", "Metals & Mining", new BigDecimal("8.40"), new BigDecimal("995.80"), new BigDecimal("-0.05"), new BigDecimal("995.20"), new BigDecimal("996.20"), 95000L, LocalDate.of(2034, 10, 14)));
        bonds.add(new BondRecord("NHAI-7.90-27", "7.90% NHAI Tax-Free Infrastructure Bond 2027", "National Highways", new BigDecimal("7.90"), new BigDecimal("1014.50"), new BigDecimal("0.03"), new BigDecimal("1014.00"), new BigDecimal("1015.00"), 320000L, LocalDate.of(2029, 10, 14)));

        String[] issuers = {
                "NABARD AAA Bond", "SIDBI Secured Bond", "EXIM Bank Infrastructure Bond", "IRFC Railway Debenture", "HUDCO Housing Bond",
                "NTPC Green Power Bond", "IOCL Petrochemical Bond", "BPCL Energy Debenture", "GAIL Pipeline Bond", "SAIL Steel Infra Bond",
                "LIC Housing Finance Bond", "HDFC Credila Education Bond", "Bajaj Finance AAA Debenture", "L&T Finance Holdings Bond", "Mahindra Finance Debenture",
                "Tata Motors Commercial Bond", "ICICI Securities Primary Bond", "Axis Finance Senior Bond", "Kotak Prime Secured Debenture", "SBI Cards Debenture",
                "Adani Ports Senior Secured", "JSW Energy Clean Bond", "ReNew Power Solar Bond", "Torrent Power Grid Bond", "Bharti Hexacom Network Bond",
                "Vedanta Aluminum Debenture", "Grasim VSF Expansion Bond", "Shree Cement Eco Bond", "UltraTech Cement Debenture", "Hindalco Copper Works Bond"
        };
        String[] prefixes = {
                "NABARD", "SIDBI", "EXIM", "IRFC", "HUDCO",
                "NTPC", "IOCL", "BPCL", "GAIL", "SAIL",
                "LICHSG", "HDFCCRED", "BAJAJ", "LTFIN", "MMFIN",
                "TATAMOT", "ICICIS", "AXISFIN", "KOTAKP", "SBICARD",
                "ADANIPT", "JSWENG", "RENEW", "TORRENT", "BHARTIH",
                "VEDL", "GRASIM", "SHREE", "ULTRATC", "HINDALCO"
        };
        String[] sectors = {
                "Financial Services", "Financial Services", "Financial Services", "PSU / Infrastructure", "PSU / Infrastructure",
                "Power & Utilities", "Energy & Petrochem", "Energy & Petrochem", "Energy & Petrochem", "Metals & Mining",
                "Financial Services", "Financial Services", "Financial Services", "Financial Services", "Financial Services",
                "Automotive & Transport", "Financial Services", "Financial Services", "Financial Services", "Financial Services",
                "PSU / Infrastructure", "Power & Utilities", "Power & Utilities", "Power & Utilities", "Telecommunications",
                "Metals & Mining", "Metals & Mining", "PSU / Infrastructure", "PSU / Infrastructure", "Metals & Mining"
        };

        for (int i = 1; i <= 1000; i++) {
            int idx = (i - 1) % 30;
            int series = ((i - 1) / 30) + 1;
            int year = 2027 + (i % 22);
            BigDecimal coupon = BigDecimal.valueOf(7.10 + ((i * 11) % 240) / 100.0).setScale(2, RoundingMode.HALF_UP);
            String sym = String.format("%s-%.2f-%d-%02d", prefixes[idx], coupon.doubleValue(), year % 100, series);
            String name = String.format("%.2f%% %s Series %d %d", coupon.doubleValue(), issuers[idx], series, year);
            String sec = sectors[idx];
            BigDecimal price = BigDecimal.valueOf(980.00 + ((i * 19) % 450) / 10.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal chg = BigDecimal.valueOf(((i * 7) % 40 - 20) / 100.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bid = price.subtract(new BigDecimal("0.50"));
            BigDecimal ask = price.add(new BigDecimal("0.50"));
            long vol = 25000L + ((i * 9800L) % 750000L);
            LocalDate mat = LocalDate.of(year, 10, 14);

            bonds.add(new BondRecord(sym, name, sec, coupon, price, chg, bid, ask, vol, mat));
        }
    }

    private void seedInMemorySecurities() {
        // Canonical 7 securities matching screenshot
        securities.add(new SecurityRecord("GS-7.18-2033", "7.18% Government of India Sovereign 2033", "G-Sec", new BigDecimal("7.18"), new BigDecimal("100.84"), new BigDecimal("0.04"), new BigDecimal("100.82"), new BigDecimal("100.86"), 8500000L, LocalDate.of(2035, 10, 14)));
        securities.add(new SecurityRecord("GS-7.06-2028", "7.06% Government of India Sovereign 2028", "G-Sec", new BigDecimal("7.06"), new BigDecimal("100.50"), new BigDecimal("0.02"), new BigDecimal("100.48"), new BigDecimal("100.52"), 4500000L, LocalDate.of(2030, 10, 14)));
        securities.add(new SecurityRecord("GS-7.26-2032", "7.26% Benchmark 10-Yr Sovereign 2032", "G-Sec", new BigDecimal("7.26"), new BigDecimal("101.15"), new BigDecimal("0.06"), new BigDecimal("101.12"), new BigDecimal("101.18"), 9800000L, LocalDate.of(2034, 10, 14)));
        securities.add(new SecurityRecord("GS-7.38-2027", "7.38% Government of India Sovereign 2027", "G-Sec", new BigDecimal("7.38"), new BigDecimal("101.40"), new BigDecimal("0.01"), new BigDecimal("101.38"), new BigDecimal("101.42"), 3800000L, LocalDate.of(2029, 10, 14)));
        securities.add(new SecurityRecord("TBILL-91D", "91-Day Sovereign Treasury Bill", "T-Bill", new BigDecimal("6.85"), new BigDecimal("98.32"), new BigDecimal("0.01"), new BigDecimal("98.31"), new BigDecimal("98.33"), 12000000L, LocalDate.of(2026, 11, 28)));
        securities.add(new SecurityRecord("TBILL-182D", "182-Day Sovereign Treasury Bill", "T-Bill", new BigDecimal("6.95"), new BigDecimal("96.65"), new BigDecimal("0.02"), new BigDecimal("96.63"), new BigDecimal("96.67"), 7500000L, LocalDate.of(2027, 2, 1)));
        securities.add(new SecurityRecord("TBILL-364D", "364-Day Sovereign Treasury Bill", "T-Bill", new BigDecimal("7.10"), new BigDecimal("93.25"), new BigDecimal("0.03"), new BigDecimal("93.22"), new BigDecimal("93.28"), 6200000L, LocalDate.of(2027, 6, 11)));

        String[] states = {
                "Maharashtra", "Gujarat", "Tamil Nadu", "Karnataka", "Uttar Pradesh",
                "Telangana", "Rajasthan", "West Bengal", "Madhya Pradesh", "Kerala",
                "Andhra Pradesh", "Punjab", "Haryana", "Odisha", "Bihar",
                "Assam", "Goa", "Jharkhand", "Chhattisgarh", "Uttarakhand"
        };
        String[] stateCodes = {
                "MH", "GJ", "TN", "KA", "UP",
                "TG", "RJ", "WB", "MP", "KL",
                "AP", "PB", "HR", "OD", "BR",
                "AS", "GA", "JH", "CG", "UK"
        };

        for (int i = 1; i <= 1000; i++) {
            int mod = i % 10;
            String cat;
            String sym;
            String desc;
            BigDecimal yield;
            BigDecimal price;
            LocalDate mat;

            if (mod < 5) {
                // Central G-Sec
                cat = "G-Sec";
                int year = 2028 + (i % 25);
                yield = BigDecimal.valueOf(6.80 + ((i * 7) % 90) / 100.0).setScale(2, RoundingMode.HALF_UP);
                sym = String.format("GS-%.2f-%d-%04d", yield.doubleValue(), year, i);
                desc = String.format("%.2f%% Government of India Sovereign %d Tranche %d", yield.doubleValue(), year, i);
                price = BigDecimal.valueOf(99.50 + ((i * 13) % 250) / 100.0).setScale(2, RoundingMode.HALF_UP);
                mat = LocalDate.of(year, 10, 14);
            } else if (mod < 8) {
                // State Development Loan
                cat = "G-Sec";
                int stIdx = (i - 1) % 20;
                int year = 2029 + (i % 18);
                yield = BigDecimal.valueOf(7.15 + ((i * 9) % 85) / 100.0).setScale(2, RoundingMode.HALF_UP);
                sym = String.format("SDL-%s-%.2f-%d-%04d", stateCodes[stIdx], yield.doubleValue(), year, i);
                desc = String.format("%.2f%% %s State Development Loan %d", yield.doubleValue(), states[stIdx], year);
                price = BigDecimal.valueOf(99.80 + ((i * 11) % 220) / 100.0).setScale(2, RoundingMode.HALF_UP);
                mat = LocalDate.of(year, 8, 20);
            } else {
                // T-Bill
                cat = "T-Bill";
                int year = 2026 + ((i % 4) + 1);
                yield = BigDecimal.valueOf(6.65 + ((i * 5) % 65) / 100.0).setScale(2, RoundingMode.HALF_UP);
                if (i % 3 == 0) {
                    sym = String.format("TBILL-91D-%d-%04d", year, i);
                    desc = String.format("91-Day Sovereign Treasury Bill Auction %d #%d", year, i);
                    price = BigDecimal.valueOf(98.10 + ((i * 3) % 40) / 100.0).setScale(2, RoundingMode.HALF_UP);
                    mat = LocalDate.of(year, 3, 15);
                } else if (i % 3 == 1) {
                    sym = String.format("TBILL-182D-%d-%04d", year, i);
                    desc = String.format("182-Day Sovereign Treasury Bill Auction %d #%d", year, i);
                    price = BigDecimal.valueOf(96.40 + ((i * 4) % 50) / 100.0).setScale(2, RoundingMode.HALF_UP);
                    mat = LocalDate.of(year, 6, 25);
                } else {
                    sym = String.format("TBILL-364D-%d-%04d", year, i);
                    desc = String.format("364-Day Sovereign Treasury Bill Auction %d #%d", year, i);
                    price = BigDecimal.valueOf(93.10 + ((i * 5) % 60) / 100.0).setScale(2, RoundingMode.HALF_UP);
                    mat = LocalDate.of(year, 11, 10);
                }
            }

            BigDecimal chg = BigDecimal.valueOf(((i * 3) % 15 - 5) / 100.0).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bid = price.subtract(new BigDecimal("0.02"));
            BigDecimal ask = price.add(new BigDecimal("0.02"));
            long vol = 1000000L + ((i * 85000L) % 15000000L);

            securities.add(new SecurityRecord(sym, desc, cat, yield, price, chg, bid, ask, vol, mat));
        }
    }

    @Override
    public MarketOverview getMarketOverview() {
        int advances = 0;
        int declines = 0;
        int unchanged = 0;
        BigDecimal totalTurnover = BigDecimal.ZERO;
        long totalVol = 0L;

        for (StockRecord s : stocks) {
            int cmp = s.changePercent().compareTo(BigDecimal.ZERO);
            if (cmp > 0) advances++;
            else if (cmp < 0) declines++;
            else unchanged++;

            totalTurnover = totalTurnover.add(s.dayTurnoverINR());
            totalVol += s.dayVolume();
        }

        String status = simulator != null && simulator.isRunning()
                ? "OPEN (Simulating: " + currentRegime.toUpperCase() + ")"
                : "OPEN (Simulated)";

        return new MarketOverview(
                status,
                Instant.now(),
                currentRegime,
                totalTurnover.compareTo(BigDecimal.ZERO) > 0 ? totalTurnover : new BigDecimal("68500000000.00"),
                totalVol > 0 ? totalVol : 18450000L,
                advances > 0 ? advances : 48,
                declines > 0 ? declines : 32,
                unchanged > 0 ? unchanged : 10,
                Collections.unmodifiableList(indices),
                Collections.unmodifiableList(marketEvents)
        );
    }

    @Override
    public List<StockRecord> getStocks(String sectorFilter, String query) {
        return stocks.stream()
                .filter(s -> sectorFilter == null || sectorFilter.equalsIgnoreCase("All") || s.sector().equalsIgnoreCase(sectorFilter))
                .filter(s -> query == null || query.isBlank() || s.symbol().toLowerCase().contains(query.toLowerCase()) || s.companyName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BondRecord> getBonds(String sectorFilter, String query) {
        return bonds.stream()
                .filter(b -> sectorFilter == null || sectorFilter.equalsIgnoreCase("All") || b.sectorType().equalsIgnoreCase(sectorFilter))
                .filter(b -> query == null || query.isBlank() || b.symbol().toLowerCase().contains(query.toLowerCase()) || b.issuerSeries().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SecurityRecord> getSecurities(String categoryFilter, String query) {
        return securities.stream()
                .filter(s -> categoryFilter == null || categoryFilter.equalsIgnoreCase("All") || s.category().equalsIgnoreCase(categoryFilter))
                .filter(s -> query == null || query.isBlank() || s.symbol().toLowerCase().contains(query.toLowerCase()) || s.securityDescription().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<InstrumentRecord> getInstruments(String categoryFilter, String query) {
        return instruments.stream()
                .filter(i -> {
                    if (categoryFilter == null || categoryFilter.equalsIgnoreCase("All")) return true;
                    if (categoryFilter.equalsIgnoreCase("Stock") || categoryFilter.equalsIgnoreCase("Stocks") || categoryFilter.equalsIgnoreCase("Equity")) {
                        return i.category().equalsIgnoreCase("Equity") || i.category().equalsIgnoreCase("Stock");
                    }
                    if (categoryFilter.equalsIgnoreCase("Bond") || categoryFilter.equalsIgnoreCase("Bonds")) {
                        return i.category().toLowerCase().contains("bond");
                    }
                    if (categoryFilter.equalsIgnoreCase("Securities")) {
                        return i.category().equalsIgnoreCase("G-Sec") || i.category().equalsIgnoreCase("T-Bill") || i.category().toLowerCase().contains("securit") || i.category().toLowerCase().contains("sovereign");
                    }
                    return i.category().equalsIgnoreCase(categoryFilter);
                })
                .filter(i -> query == null || query.isBlank() || i.symbol().toLowerCase().contains(query.toLowerCase()) || i.name().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InstrumentRecord> getInstrument(String symbol) {
        return instruments.stream().filter(i -> i.symbol().equalsIgnoreCase(symbol)).findFirst();
    }

    @Override
    public List<OrderBookLevel> getOrderBookDepth(String symbol) {
        InstrumentRecord inst = getInstrument(symbol).orElse(instruments.get(0));
        BigDecimal base = inst.lastTradedPrice();
        BigDecimal tick = inst.tickSize();

        BigDecimal spreadMult = simulator != null ? simulator.getSpreadMultiplier() : BigDecimal.ONE;
        BigDecimal step = tick.multiply(spreadMult).setScale(2, RoundingMode.HALF_UP);
        if (step.compareTo(tick) < 0) step = tick;

        double bidWeight = simulator != null ? simulator.getBidQueueWeight() : 1.0;
        double askWeight = simulator != null ? simulator.getAskQueueWeight() : 1.0;

        List<OrderBookLevel> levels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            BigDecimal offset = step.multiply(BigDecimal.valueOf(i));
            BigDecimal bPrice = base.subtract(offset).setScale(2, RoundingMode.HALF_UP);
            if (bPrice.compareTo(BigDecimal.ZERO) <= 0) bPrice = new BigDecimal("0.05");
            BigDecimal aPrice = base.add(offset).setScale(2, RoundingMode.HALF_UP);

            long baseBidQty = Math.max(50, (long) ((1500 + i * 2200) * bidWeight));
            long baseAskQty = Math.max(50, (long) ((1800 + i * 2000) * askWeight));
            int bidOrders = Math.max(1, (int) (12 * bidWeight) + i * 3);
            int askOrders = Math.max(1, (int) (12 * askWeight) + i * 3);

            levels.add(new OrderBookLevel(i, bidOrders, baseBidQty, bPrice, aPrice, baseAskQty, askOrders));
        }
        return levels;
    }

    @Override
    public List<OrderRecord> getOrderBlotter(String statusFilter) {
        return orders.stream()
                .filter(o -> statusFilter == null || statusFilter.equalsIgnoreCase("All") || o.status().equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeRecord> getTradeBlotter() {
        return Collections.unmodifiableList(trades);
    }

    @Override
    public OrderRecord submitSimulatedOrder(String symbol, String side, String orderType, long quantity, BigDecimal price, String tif) {
        InstrumentRecord inst = getInstrument(symbol).orElseThrow();
        BigDecimal effectivePrice = orderType.equalsIgnoreCase("MARKET") ? (side.equalsIgnoreCase("BUY") ? inst.askPrice() : inst.bidPrice()) : price;
        BigDecimal notional = effectivePrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        String ordId = "ORD-" + (System.currentTimeMillis() % 100000);
        OrderRecord order = new OrderRecord(
                ordId,
                "DEMO-CLIENT-" + (System.currentTimeMillis() % 1000),
                Instant.now(),
                symbol,
                side.toUpperCase(),
                orderType.toUpperCase(),
                quantity,
                quantity, // instant simulated matching for demonstration
                effectivePrice,
                effectivePrice,
                "Filled",
                tif,
                notional
        );
        orders.add(0, order);

        TradeRecord trade = new TradeRecord(
                "TRD-" + (System.currentTimeMillis() % 100000),
                ordId,
                Instant.now(),
                symbol,
                side.toUpperCase(),
                quantity,
                effectivePrice,
                notional
        );
        trades.add(0, trade);

        marketEvents.add(0, "Simulated execution: " + side + " " + quantity + " " + symbol + " @ ₹ " + effectivePrice);
        return order;
    }

    @Override
    public boolean cancelSimulatedOrder(String orderId) {
        for (int i = 0; i < orders.size(); i++) {
            OrderRecord o = orders.get(i);
            if (o.orderId().equalsIgnoreCase(orderId) && !o.status().equalsIgnoreCase("Filled") && !o.status().equalsIgnoreCase("Cancelled")) {
                orders.set(i, new OrderRecord(
                        o.orderId(), o.clientRefId(), o.placedAt(), o.symbol(), o.side(), o.orderType(),
                        o.totalQuantity(), o.filledQuantity(), o.limitPrice(), o.averageFillPrice(), "Cancelled", o.timeInForce(), o.estimatedNotionalINR()
                ));
                marketEvents.add(0, "Order " + orderId + " cancelled by participant.");
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateMarketRegime(String newRegime, int speedFactor, long seed) {
        this.currentRegime = newRegime;
        this.simulationSpeed = speedFactor;
        this.currentSeed = seed;
        if (simulator != null) {
            simulator.setRegime(newRegime, speedFactor, seed);
        } else {
            marketEvents.add(0, "Market regime altered to " + newRegime.toUpperCase() + " (Speed: " + speedFactor + "x, Seed: " + seed + ")");
        }
    }

    @Override
    public ExchangeMarketSimulator getSimulator() {
        return simulator;
    }

    @Override
    public boolean isSimulationRunning() {
        return simulator != null && simulator.isRunning();
    }

    @Override
    public void startSimulation() {
        if (simulator != null) {
            simulator.start();
        }
    }

    @Override
    public void pauseSimulation() {
        if (simulator != null) {
            simulator.pause();
        }
    }

    @Override
    public void resumeSimulation() {
        if (simulator != null) {
            simulator.resume();
        }
    }

    @Override
    public String getCurrentRegime() {
        return currentRegime;
    }

    @Override
    public void addDataChangeListener(Runnable listener) {
        if (simulator != null) {
            simulator.addDataChangeListener(listener);
        }
    }

    public void recordMarketEvent(String event) {
        marketEvents.add(0, event);
        if (marketEvents.size() > 200) {
            marketEvents.remove(marketEvents.size() - 1);
        }
    }

    public void rebuildLookupIndices() {
        stockIndices.clear();
        for (int i = 0; i < stocks.size(); i++) {
            StockRecord s = stocks.get(i);
            stockIndices.put(s.symbol().toUpperCase(), i);
            basePrices.putIfAbsent(s.symbol().toUpperCase(), s.lastTradedPrice());
        }
        bondIndices.clear();
        for (int i = 0; i < bonds.size(); i++) {
            BondRecord b = bonds.get(i);
            bondIndices.put(b.symbol().toUpperCase(), i);
            basePrices.putIfAbsent(b.symbol().toUpperCase(), b.cleanPrice());
        }
        securityIndices.clear();
        for (int i = 0; i < securities.size(); i++) {
            SecurityRecord sc = securities.get(i);
            securityIndices.put(sc.symbol().toUpperCase(), i);
            basePrices.putIfAbsent(sc.symbol().toUpperCase(), sc.cleanPrice());
        }
        instrumentIndices.clear();
        for (int i = 0; i < instruments.size(); i++) {
            InstrumentRecord inst = instruments.get(i);
            instrumentIndices.put(inst.symbol().toUpperCase(), i);
            basePrices.putIfAbsent(inst.symbol().toUpperCase(), inst.lastTradedPrice());
        }
    }

    public void recordTickExecution(String symbol, BigDecimal newLtp, BigDecimal newBid, BigDecimal newAsk,
                                    long quantity, BigDecimal notional, OrderRecord order, TradeRecord trade) {
        String symUpper = symbol.toUpperCase();

        // 1. Update Stock if applicable
        Integer sIdx = stockIndices.get(symUpper);
        if (sIdx != null && sIdx < stocks.size()) {
            StockRecord cur = stocks.get(sIdx);
            BigDecimal base = basePrices.getOrDefault(symUpper, cur.lastTradedPrice());
            BigDecimal chgPct = base.compareTo(BigDecimal.ZERO) != 0
                    ? newLtp.subtract(base).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            long newVol = cur.dayVolume() + quantity;
            BigDecimal newTurnover = cur.dayTurnoverINR().add(notional);
            stocks.set(sIdx, new StockRecord(cur.symbol(), cur.companyName(), cur.sector(), newLtp, chgPct, newBid, newAsk, newVol, newTurnover));
        }

        // 2. Update Bond if applicable
        Integer bIdx = bondIndices.get(symUpper);
        if (bIdx != null && bIdx < bonds.size()) {
            BondRecord cur = bonds.get(bIdx);
            BigDecimal base = basePrices.getOrDefault(symUpper, cur.cleanPrice());
            BigDecimal chgPct = base.compareTo(BigDecimal.ZERO) != 0
                    ? newLtp.subtract(base).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            long newVol = cur.dayVolume() + quantity;
            bonds.set(bIdx, new BondRecord(cur.symbol(), cur.issuerSeries(), cur.sectorType(), cur.couponRatePercent(), newLtp, chgPct, newBid, newAsk, newVol, cur.maturityDate()));
        }

        // 3. Update Security if applicable
        Integer scIdx = securityIndices.get(symUpper);
        if (scIdx != null && scIdx < securities.size()) {
            SecurityRecord cur = securities.get(scIdx);
            BigDecimal base = basePrices.getOrDefault(symUpper, cur.cleanPrice());
            BigDecimal chgPct = base.compareTo(BigDecimal.ZERO) != 0
                    ? newLtp.subtract(base).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            long newVol = cur.dayVolume() + quantity;
            securities.set(scIdx, new SecurityRecord(cur.symbol(), cur.securityDescription(), cur.category(), cur.benchmarkYieldPercent(), newLtp, chgPct, newBid, newAsk, newVol, cur.maturityDate()));
        }

        // 4. Update Instrument Record
        Integer instIdx = instrumentIndices.get(symUpper);
        if (instIdx != null && instIdx < instruments.size()) {
            InstrumentRecord cur = instruments.get(instIdx);
            BigDecimal base = basePrices.getOrDefault(symUpper, cur.lastTradedPrice());
            BigDecimal chgPct = base.compareTo(BigDecimal.ZERO) != 0
                    ? newLtp.subtract(base).divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            long newVol = cur.dayVolume() + quantity;
            BigDecimal newTurnover = cur.dayTurnoverINR().add(notional);
            instruments.set(instIdx, new InstrumentRecord(
                    cur.isinOrId(), cur.symbol(), cur.name(), cur.category(), cur.sector(),
                    cur.tickSize(), cur.lotSize(), newLtp, chgPct, newBid, newAsk, newVol, newTurnover,
                    Instant.now(), cur.couponOrYieldPercent(), cur.maturityOrExpiryDate(), cur.strikePrice(), cur.optionType()
            ));
        }

        // 5. Update Indices
        updateBenchmarkIndices();

        // 6. Record Order & Trade
        if (order != null) {
            orders.add(0, order);
            if (orders.size() > 500) {
                orders.remove(orders.size() - 1);
            }
        }
        if (trade != null) {
            trades.add(0, trade);
            if (trades.size() > 500) {
                trades.remove(trades.size() - 1);
            }
        }
    }

    private void updateBenchmarkIndices() {
        if (indices.size() >= 3 && !stocks.isEmpty()) {
            // Nifty 50 approximation based on top constituent stocks
            double niftySumChange = 0.0;
            int count = 0;
            for (String sym : new String[]{"RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "BHARTIARTL", "LT", "TATAMOTORS", "ITC"}) {
                Integer idx = stockIndices.get(sym);
                if (idx != null && idx < stocks.size()) {
                    niftySumChange += stocks.get(idx).changePercent().doubleValue();
                    count++;
                }
            }
            double avgChange = count > 0 ? (niftySumChange / count) : 0.0;

            BigDecimal nBase = new BigDecimal("24800.00");
            BigDecimal nLvl = nBase.multiply(BigDecimal.valueOf(1.0 + (avgChange / 100.0))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal nChg = BigDecimal.valueOf(avgChange).setScale(2, RoundingMode.HALF_UP);

            indices.set(0, new MarketIndex("SIM-NIFTY50", "Simulated Nifty 50 Index", nLvl, nChg, nLvl.add(new BigDecimal("120")), nLvl.subtract(new BigDecimal("90"))));

            // Bank Nifty approximation
            double bankSumChange = 0.0;
            int bCount = 0;
            for (String sym : new String[]{"HDFCBANK", "ICICIBANK", "SBIN", "KOTAKBANK", "AXISBANK"}) {
                Integer idx = stockIndices.get(sym);
                if (idx != null && idx < stocks.size()) {
                    bankSumChange += stocks.get(idx).changePercent().doubleValue();
                    bCount++;
                }
            }
            double bAvg = bCount > 0 ? (bankSumChange / bCount) : avgChange;
            BigDecimal bBase = new BigDecimal("51200.00");
            BigDecimal bLvl = bBase.multiply(BigDecimal.valueOf(1.0 + (bAvg / 100.0))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bChg = BigDecimal.valueOf(bAvg).setScale(2, RoundingMode.HALF_UP);
            indices.set(1, new MarketIndex("SIM-BANKNIFTY", "Simulated Bank Nifty Index", bLvl, bChg, bLvl.add(new BigDecimal("250")), bLvl.subtract(new BigDecimal("210"))));

            // Midcap
            BigDecimal mBase = new BigDecimal("56100.00");
            BigDecimal mLvl = mBase.multiply(BigDecimal.valueOf(1.0 + (avgChange * 1.2 / 100.0))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal mChg = BigDecimal.valueOf(avgChange * 1.2).setScale(2, RoundingMode.HALF_UP);
            indices.set(2, new MarketIndex("SIM-MIDCAP", "Simulated Nifty Midcap 100", mLvl, mChg, mLvl.add(new BigDecimal("300")), mLvl.subtract(new BigDecimal("240"))));
        }
    }
}
