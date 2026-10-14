package com.lro.frontend.exchange.service;

import com.lro.frontend.exchange.model.ExchangeModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final List<MarketIndex> indices = new ArrayList<>();
    private final List<InstrumentRecord> instruments = new ArrayList<>();
    private final List<OrderRecord> orders = new CopyOnWriteArrayList<>();
    private final List<TradeRecord> trades = new CopyOnWriteArrayList<>();
    private final List<String> marketEvents = new CopyOnWriteArrayList<>();

    public MockExchangeService() {
        seedExchangeData();
    }

    private void seedExchangeData() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        indices.add(new MarketIndex("SIM-NIFTY50", "Simulated Nifty 50 Index", new BigDecimal("24850.40"), new BigDecimal("0.65"), new BigDecimal("24920.00"), new BigDecimal("24780.10")));
        indices.add(new MarketIndex("SIM-BANKNIFTY", "Simulated Bank Nifty Index", new BigDecimal("51420.80"), new BigDecimal("1.12"), new BigDecimal("51600.00"), new BigDecimal("51100.50")));
        indices.add(new MarketIndex("SIM-MIDCAP", "Simulated Nifty Midcap 100", new BigDecimal("56200.15"), new BigDecimal("-0.24"), new BigDecimal("56450.00"), new BigDecimal("55980.00")));

        // 1. Equities
        instruments.add(new InstrumentRecord("INE002A01018-SIM", "RELIANCE", "Reliance Industries (Simulated)", "Equity", "Energy / Conglomerate", new BigDecimal("0.05"), 1, new BigDecimal("2840.50"), new BigDecimal("1.20"), new BigDecimal("2840.25"), new BigDecimal("2840.75"), 1845000L, new BigDecimal("5240000000.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("INE040A01034-SIM", "HDFCBANK", "HDFC Bank Ltd (Simulated)", "Equity", "Banking & Financials", new BigDecimal("0.05"), 1, new BigDecimal("1685.20"), new BigDecimal("0.85"), new BigDecimal("1685.00"), new BigDecimal("1685.50"), 2450000L, new BigDecimal("4128000000.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("INE009A01021-SIM", "INFY", "Infosys Ltd (Simulated)", "Equity", "Information Technology", new BigDecimal("0.05"), 1, new BigDecimal("1820.75"), new BigDecimal("-0.45"), new BigDecimal("1820.50"), new BigDecimal("1821.00"), 1230000L, new BigDecimal("2239000000.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("INE467B01029-SIM", "TCS", "Tata Consultancy Services (Simulated)", "Equity", "Information Technology", new BigDecimal("0.05"), 1, new BigDecimal("4150.00"), new BigDecimal("0.30"), new BigDecimal("4149.50"), new BigDecimal("4150.50"), 820000L, new BigDecimal("3403000000.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("INE090A01021-SIM", "ICICIBANK", "ICICI Bank Ltd (Simulated)", "Equity", "Banking & Financials", new BigDecimal("0.05"), 1, new BigDecimal("1210.40"), new BigDecimal("1.45"), new BigDecimal("1210.10"), new BigDecimal("1210.60"), 3120000L, new BigDecimal("3776000000.00"), now, null, null, null, null));

        // 2. Government Securities & T-Bills
        instruments.add(new InstrumentRecord("IN0020230085-SIM", "GS-7.18-2033", "7.18% Government of India 2033", "G-Sec", "Sovereign Debt", new BigDecimal("0.01"), 10000, new BigDecimal("100.84"), new BigDecimal("0.04"), new BigDecimal("100.82"), new BigDecimal("100.86"), 8500000L, new BigDecimal("857140000.00"), now, new BigDecimal("7.18"), today.plusYears(9), null, null));
        instruments.add(new InstrumentRecord("IN0020180025-SIM", "GS-7.06-2028", "7.06% Government of India 2028", "G-Sec", "Sovereign Debt", new BigDecimal("0.01"), 10000, new BigDecimal("100.50"), new BigDecimal("0.02"), new BigDecimal("100.48"), new BigDecimal("100.52"), 4500000L, new BigDecimal("452250000.00"), now, new BigDecimal("7.06"), today.plusYears(4), null, null));
        instruments.add(new InstrumentRecord("IN0091TB2401-SIM", "TBILL-91D", "91-Day Sovereign Treasury Bill", "T-Bill", "Money Market", new BigDecimal("0.01"), 10000, new BigDecimal("98.32"), new BigDecimal("0.01"), new BigDecimal("98.31"), new BigDecimal("98.33"), 12000000L, new BigDecimal("1179840000.00"), now, new BigDecimal("6.85"), today.plusDays(45), null, null));

        // 3. Corporate Bonds & Money Market
        instruments.add(new InstrumentRecord("INE040A08427-SIM", "HDFCB-8.15-29", "8.15% HDFC Bank AAA Bond 2029", "Corporate Bond", "Financial Services", new BigDecimal("0.05"), 100, new BigDecimal("998.00"), new BigDecimal("-0.10"), new BigDecimal("997.50"), new BigDecimal("998.50"), 150000L, new BigDecimal("149700000.00"), now, new BigDecimal("8.15"), today.plusYears(5), null, null));

        // 4. Derivatives (Futures & Options)
        instruments.add(new InstrumentRecord("FUT-NIFTY-26OCT-SIM", "NIFTY-FUT-1M", "Nifty Index Future Oct 2026", "Futures", "Index Derivatives", new BigDecimal("0.05"), 25, new BigDecimal("24910.00"), new BigDecimal("0.70"), new BigDecimal("24909.50"), new BigDecimal("24911.00"), 340000L, new BigDecimal("8469000000.00"), now, null, today.plusDays(20), null, null));
        instruments.add(new InstrumentRecord("OPT-NIFTY-25000CE-SIM", "NIFTY-25000-CE", "Nifty 25000 Call Option", "Options", "Index Derivatives", new BigDecimal("0.05"), 25, new BigDecimal("145.20"), new BigDecimal("8.50"), new BigDecimal("145.00"), new BigDecimal("145.50"), 1200000L, new BigDecimal("174240000.00"), now, null, today.plusDays(20), new BigDecimal("25000.00"), "CE"));

        // 5. FX & Commodities
        instruments.add(new InstrumentRecord("FX-USDINR-SIM", "USDINR-SPOT", "US Dollar / Indian Rupee", "FX", "Currency", new BigDecimal("0.0025"), 1000, new BigDecimal("83.9850"), new BigDecimal("0.05"), new BigDecimal("83.9825"), new BigDecimal("83.9875"), 5500000L, new BigDecimal("461917500.00"), now, null, null, null, null));
        instruments.add(new InstrumentRecord("COM-GOLD-10G-SIM", "GOLD-10G", "Gold 999 10 Grams (Simulated)", "Commodity", "Precious Metals", new BigDecimal("1.00"), 1, new BigDecimal("75800.00"), new BigDecimal("0.40"), new BigDecimal("75790.00"), new BigDecimal("75810.00"), 42000L, new BigDecimal("3183600000.00"), now, null, null, null, null));

        // Seed Orders and Trades
        orders.add(new OrderRecord("ORD-1001", "CL-DEMO-01", now.minusSeconds(1200), "RELIANCE", "BUY", "LIMIT", 500, 500, new BigDecimal("2840.00"), new BigDecimal("2840.00"), "Filled", "DAY", new BigDecimal("1420000.00")));
        orders.add(new OrderRecord("ORD-1002", "CL-DEMO-02", now.minusSeconds(800), "HDFCBANK", "SELL", "LIMIT", 1000, 400, new BigDecimal("1686.00"), new BigDecimal("1686.00"), "Partially Filled", "DAY", new BigDecimal("1686000.00")));
        orders.add(new OrderRecord("ORD-1003", "CL-DEMO-03", now.minusSeconds(300), "GS-7.18-2033", "BUY", "LIMIT", 50000, 0, new BigDecimal("100.80"), BigDecimal.ZERO, "Accepted", "DAY", new BigDecimal("5040000.00")));

        trades.add(new TradeRecord("TRD-9001", "ORD-1001", now.minusSeconds(1190), "RELIANCE", "BUY", 500, new BigDecimal("2840.00"), new BigDecimal("1420000.00")));
        trades.add(new TradeRecord("TRD-9002", "ORD-1002", now.minusSeconds(790), "HDFCBANK", "SELL", 400, new BigDecimal("1686.00"), new BigDecimal("674400.00")));

        marketEvents.add("Simulated Morning Auction completed cleanly.");
        marketEvents.add("RBI Monetary Policy Stance simulated in Normal regime.");
        marketEvents.add("Sovereign G-Sec matching active with tight bid-ask spreads.");
    }

    @Override
    public MarketOverview getMarketOverview() {
        return new MarketOverview(
                "OPEN (Simulated)",
                Instant.now(),
                currentRegime,
                new BigDecimal("68500000000.00"), // ₹ 6,850 Cr turnover
                18450000L,
                48,
                32,
                10,
                Collections.unmodifiableList(indices),
                Collections.unmodifiableList(marketEvents)
        );
    }

    @Override
    public List<InstrumentRecord> getInstruments(String categoryFilter, String query) {
        return instruments.stream()
                .filter(i -> categoryFilter == null || categoryFilter.equalsIgnoreCase("All") || i.category().equalsIgnoreCase(categoryFilter))
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

        List<OrderBookLevel> levels = new ArrayList<>();
        levels.add(new OrderBookLevel(1, 14, 2500L, base.subtract(tick), base.add(tick), 3100L, 19));
        levels.add(new OrderBookLevel(2, 22, 5200L, base.subtract(tick.multiply(new BigDecimal("2"))), base.add(tick.multiply(new BigDecimal("2"))), 6400L, 27));
        levels.add(new OrderBookLevel(3, 31, 8900L, base.subtract(tick.multiply(new BigDecimal("3"))), base.add(tick.multiply(new BigDecimal("3"))), 7800L, 24));
        levels.add(new OrderBookLevel(4, 18, 4100L, base.subtract(tick.multiply(new BigDecimal("4"))), base.add(tick.multiply(new BigDecimal("4"))), 5900L, 16));
        levels.add(new OrderBookLevel(5, 45, 12500L, base.subtract(tick.multiply(new BigDecimal("5"))), base.add(tick.multiply(new BigDecimal("5"))), 14200L, 38));
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
        marketEvents.add(0, "Market regime altered to " + newRegime.toUpperCase() + " (Speed: " + speedFactor + "x, Seed: " + seed + ")");
    }
}
