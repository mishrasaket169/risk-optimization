package com.lro.frontend.exchange.service;

import com.lro.frontend.exchange.model.ExchangeModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-Time Continuous Exchange Market Simulator.
 * Simulates continuous double-auction matching engine microstructure,
 * dynamic order book depth, ticks, and trade executions according to
 * defined market regimes: NORMAL, BULL, BEAR, and CRASH.
 */
public class ExchangeMarketSimulator {

    private static final Logger log = LoggerFactory.getLogger(ExchangeMarketSimulator.class);

    public enum MarketRegime {
        NORMAL(new BigDecimal("1.00"), new BigDecimal("1.00"), 0.50, 0.0000, 0.0015, -0.020, 0.020, 0.000, 1.0, 1.0, "Normal continuous auction matching: balanced liquidity and two-way order flow"),
        BULL(new BigDecimal("0.85"), new BigDecimal("1.25"), 0.74, 0.0018, 0.0020, 0.005, 0.090, 0.045, 2.4, 0.6, "Bull regime: strong buying momentum, tight spreads, heavy bid depth, green rally"),
        BEAR(new BigDecimal("1.60"), new BigDecimal("1.75"), 0.26, -0.0020, 0.0026, -0.090, -0.005, -0.045, 0.6, 2.2, "Bear regime: institutional selling pressure, wider spreads, red market breadth"),
        CRASH(new BigDecimal("3.80"), new BigDecimal("4.50"), 0.10, -0.0060, 0.0065, -0.200, -0.040, -0.120, 0.15, 4.5, "CRASH REGIME: panic liquidation, severe illiquidity, circuit warning, spreads blown out 3.8x");

        private final BigDecimal spreadMultiplier;
        private final BigDecimal volatilityFactor;
        private final double buyProbability;
        private final double priceDriftMean;
        private final double priceDriftStdDev;
        private final double minReturnBound;
        private final double maxReturnBound;
        private final double targetReturn;
        private final double bidQueueWeight;
        private final double askQueueWeight;
        private final String description;

        MarketRegime(BigDecimal spreadMultiplier, BigDecimal volatilityFactor, double buyProbability,
                     double priceDriftMean, double priceDriftStdDev,
                     double minReturnBound, double maxReturnBound, double targetReturn,
                     double bidQueueWeight, double askQueueWeight, String description) {
            this.spreadMultiplier = spreadMultiplier;
            this.volatilityFactor = volatilityFactor;
            this.buyProbability = buyProbability;
            this.priceDriftMean = priceDriftMean;
            this.priceDriftStdDev = priceDriftStdDev;
            this.minReturnBound = minReturnBound;
            this.maxReturnBound = maxReturnBound;
            this.targetReturn = targetReturn;
            this.bidQueueWeight = bidQueueWeight;
            this.askQueueWeight = askQueueWeight;
            this.description = description;
        }

        public BigDecimal getSpreadMultiplier() { return spreadMultiplier; }
        public BigDecimal getVolatilityFactor() { return volatilityFactor; }
        public double getBuyProbability() { return buyProbability; }
        public double getPriceDriftMean() { return priceDriftMean; }
        public double getPriceDriftStdDev() { return priceDriftStdDev; }
        public double getMinReturnBound() { return minReturnBound; }
        public double getMaxReturnBound() { return maxReturnBound; }
        public double getTargetReturn() { return targetReturn; }
        public double getBidQueueWeight() { return bidQueueWeight; }
        public double getAskQueueWeight() { return askQueueWeight; }
        public String getDescription() { return description; }

        public static MarketRegime fromString(String name) {
            if (name == null) return NORMAL;
            for (MarketRegime r : values()) {
                if (r.name().equalsIgnoreCase(name.trim())) {
                    return r;
                }
            }
            return NORMAL;
        }
    }

    private final MockExchangeService exchangeService;
    private final DataSource dataSource;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong tickCount = new AtomicLong(0);
    private final AtomicLong tradesCount = new AtomicLong(0);

    private volatile MarketRegime currentRegime = MarketRegime.NORMAL;
    private volatile int simulationSpeed = 1; // 1 to 10
    private volatile long currentSeed = 42L;

    private Random random;
    private ScheduledExecutorService executorService;
    private final List<Runnable> dataChangeListeners = new CopyOnWriteArrayList<>();

    // Prominent liquid equities for priority simulation
    private static final String[] PRIORITY_SYMBOLS = {
            "RELIANCE", "HDFCBANK", "INFY", "TCS", "ICICIBANK",
            "SBIN", "BHARTIARTL", "LT", "TATAMOTORS", "ITC",
            "KOTAKBANK", "AXISBANK", "BAJFINANCE", "MARUTI", "SUNPHARMA"
    };

    private static final String[] CLIENT_DESKS = {
            "INST-FII-MUMBAI", "DII-LIC-INDIA", "MUTUAL-FUND-ALPHA", "TREASURY-HDFC",
            "ALGO-HFT-SPEED", "RETAIL-HNI-42", "ARBITRAGE-DESK-NSE", "PROP-TRADING-09"
    };

    public ExchangeMarketSimulator(MockExchangeService exchangeService, DataSource dataSource) {
        this.exchangeService = exchangeService;
        this.dataSource = dataSource;
        this.random = new Random(currentSeed);
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting Exchange Market Simulator in {} regime at {}x speed...", currentRegime, simulationSpeed);
            exchangeService.recordMarketEvent("EXCHANGE SIMULATOR STARTED: Mode=" + currentRegime.name() + " | Speed=" + simulationSpeed + "x | Seed=" + currentSeed);

            executorService = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "Lro-Exchange-Simulator");
                t.setDaemon(true);
                return t;
            });

            scheduleSimulationTicks();
        }
    }

    private synchronized void scheduleSimulationTicks() {
        if (executorService != null && !executorService.isShutdown()) {
            // Speed factor: 1x = ~200ms per tick (5 ticks/sec), 5x = ~50ms (20 ticks/sec), 10x = ~20ms (50 ticks/sec)
            long intervalMillis = Math.max(20, 200 / simulationSpeed);
            executorService.scheduleAtFixedRate(this::executeSimulationTick, 0, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void pause() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Pausing Exchange Market Simulator.");
            exchangeService.recordMarketEvent("EXCHANGE SIMULATOR PAUSED: Market matching temporarily suspended.");
            if (executorService != null) {
                executorService.shutdownNow();
            }
            notifyDataChanged();
        }
    }

    public synchronized void resume() {
        start();
    }

    public synchronized void stop() {
        pause();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public MarketRegime getRegime() {
        return currentRegime;
    }

    public BigDecimal getSpreadMultiplier() {
        return currentRegime.getSpreadMultiplier();
    }

    public double getBidQueueWeight() {
        return currentRegime.getBidQueueWeight();
    }

    public double getAskQueueWeight() {
        return currentRegime.getAskQueueWeight();
    }

    public int getSimulationSpeed() {
        return simulationSpeed;
    }

    public long getCurrentSeed() {
        return currentSeed;
    }

    public long getTickCount() {
        return tickCount.get();
    }

    public long getTradesCount() {
        return tradesCount.get();
    }

    public synchronized void setRegime(String newRegimeName, int speedFactor, long seed) {
        MarketRegime prevRegime = this.currentRegime;
        this.currentRegime = MarketRegime.fromString(newRegimeName);
        this.simulationSpeed = Math.max(1, Math.min(10, speedFactor));
        this.currentSeed = seed;
        this.random = new Random(seed);

        // Re-align all market stocks, depth quotes, and indices to the newly defined regime
        exchangeService.realignMarketToRegime(this.currentRegime, this.random);

        String eventMsg = String.format("MARKET REGIME SHIFT: %s -> %s (Speed: %dx, Seed: %d) — Spread Multiplier: %sx, Volatility: %sx",
                prevRegime.name(), currentRegime.name(), simulationSpeed, seed,
                currentRegime.getSpreadMultiplier(), currentRegime.getVolatilityFactor());

        log.info(eventMsg);
        exchangeService.recordMarketEvent(eventMsg);
        exchangeService.recordMarketEvent("REGIME CHARACTERISTIC: " + currentRegime.getDescription());

        if (isRunning.get()) {
            // Reschedule at new speed
            if (executorService != null) {
                executorService.shutdownNow();
            }
            executorService = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "Lro-Exchange-Simulator");
                t.setDaemon(true);
                return t;
            });
            scheduleSimulationTicks();
        }

        notifyDataChanged();
    }

    public void addDataChangeListener(Runnable listener) {
        if (listener != null) {
            dataChangeListeners.add(listener);
        }
    }

    private void notifyDataChanged() {
        for (Runnable r : dataChangeListeners) {
            try {
                r.run();
            } catch (Exception ex) {
                log.debug("Listener update error: {}", ex.getMessage());
            }
        }
    }

    /**
     * Advances the simulation by a fixed number of ticks (useful for interactive step-testing).
     */
    public synchronized void step(int count) {
        int ticksToRun = Math.max(1, count);
        for (int i = 0; i < ticksToRun; i++) {
            executeSimulationTick();
        }
        notifyDataChanged();
    }

    /**
     * Calculates realistic regime-linked prices, spreads, and quotes.
     * Incorporates drift, order pressure, mean-reversion toward regime target, and volatility bounds.
     */
    private BigDecimal[] calculateRegimePrice(String symbol, BigDecimal currentLtp, BigDecimal tickSize, boolean isBuy) {
        BigDecimal base = exchangeService.getBasePrice(symbol);
        double r = base.compareTo(BigDecimal.ZERO) != 0
                ? currentLtp.subtract(base).divide(base, 6, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double baseDrift = currentRegime.getPriceDriftMean() + (random.nextGaussian() * currentRegime.getPriceDriftStdDev());
        double volFactor = currentRegime.getVolatilityFactor().doubleValue();
        double effectiveDrift = baseDrift * volFactor;

        double sidePressure = (isBuy ? 0.0004 : -0.0004) * volFactor;
        double pull = (currentRegime.getTargetReturn() - r) * 0.05;

        double shock = 0.0;
        if (currentRegime == MarketRegime.CRASH && random.nextDouble() < 0.12) {
            shock = -(0.015 + random.nextDouble() * 0.035);
        } else if (currentRegime == MarketRegime.BULL && random.nextDouble() < 0.10) {
            shock = +(0.008 + random.nextDouble() * 0.018);
        }

        double rNew = r + effectiveDrift + sidePressure + pull + shock;
        rNew = Math.max(currentRegime.getMinReturnBound(), Math.min(currentRegime.getMaxReturnBound(), rNew));

        BigDecimal rawNewPrice = base.multiply(BigDecimal.valueOf(1.0 + rNew));
        long tickUnits = Math.round(rawNewPrice.divide(tickSize, 0, RoundingMode.HALF_UP).doubleValue());
        BigDecimal newLtp = tickSize.multiply(BigDecimal.valueOf(tickUnits)).setScale(2, RoundingMode.HALF_UP);
        if (newLtp.compareTo(tickSize) < 0) {
            newLtp = tickSize;
        }

        BigDecimal spreadMult = currentRegime.getSpreadMultiplier();
        long halfSpreadTicks = Math.max(1, Math.round(spreadMult.doubleValue() * 2.0));
        BigDecimal halfSpread = tickSize.multiply(BigDecimal.valueOf(halfSpreadTicks));

        BigDecimal newBid;
        BigDecimal newAsk;
        if (currentRegime == MarketRegime.CRASH) {
            newBid = newLtp.subtract(halfSpread.multiply(new BigDecimal("1.5"))).setScale(2, RoundingMode.HALF_UP);
            newAsk = newLtp.add(halfSpread.multiply(new BigDecimal("0.5"))).setScale(2, RoundingMode.HALF_UP);
        } else {
            newBid = newLtp.subtract(halfSpread).setScale(2, RoundingMode.HALF_UP);
            newAsk = newLtp.add(halfSpread).setScale(2, RoundingMode.HALF_UP);
        }
        if (newBid.compareTo(BigDecimal.ZERO) <= 0) {
            newBid = new BigDecimal("0.05");
        }
        return new BigDecimal[]{newLtp, newBid, newAsk};
    }

    /**
     * Executes one discrete simulation tick in the continuous matching engine.
     */
    public void executeSimulationTick() {
        try {
            long currentTick = tickCount.incrementAndGet();

            // 1. Pick primary active instrument (weighted 80% to active equities, 20% to broader catalogue)
            String symbol;
            if (random.nextDouble() < 0.80) {
                symbol = PRIORITY_SYMBOLS[random.nextInt(PRIORITY_SYMBOLS.length)];
            } else {
                List<StockRecord> allStocks = exchangeService.getStocks("All", "");
                if (!allStocks.isEmpty()) {
                    symbol = allStocks.get(random.nextInt(Math.min(100, allStocks.size()))).symbol();
                } else {
                    symbol = PRIORITY_SYMBOLS[0];
                }
            }

            Optional<InstrumentRecord> instOpt = exchangeService.getInstrument(symbol);
            if (instOpt.isEmpty()) return;
            InstrumentRecord inst = instOpt.get();

            BigDecimal currentLtp = inst.lastTradedPrice();
            BigDecimal tickSize = inst.tickSize();

            // 2. Regime-driven side determination
            boolean isBuy = random.nextDouble() < currentRegime.getBuyProbability();
            String side = isBuy ? "BUY" : "SELL";

            // 3. Regime-driven price calculation with mean-reversion toward target return
            BigDecimal[] quotes = calculateRegimePrice(symbol, currentLtp, tickSize, isBuy);
            BigDecimal newLtp = quotes[0];
            BigDecimal newBid = quotes[1];
            BigDecimal newAsk = quotes[2];

            // 4. Generate realistic lot quantity
            long quantity;
            if (currentRegime == MarketRegime.CRASH && !isBuy && random.nextDouble() < 0.25) {
                quantity = (1000 + random.nextInt(9) * 500L); // Institutional panic liquidation block
            } else if (currentRegime == MarketRegime.BULL && isBuy && random.nextDouble() < 0.30) {
                quantity = (200 + random.nextInt(10) * 100L); // Institutional accumulation block
            } else {
                quantity = (25 + random.nextInt(15) * 25L);
            }

            String orderType = (currentRegime == MarketRegime.CRASH && !isBuy) ? "MARKET" : (random.nextDouble() < 0.85 ? "LIMIT" : "MARKET");
            String client = CLIENT_DESKS[random.nextInt(CLIENT_DESKS.length)];

            // 5. Update in-memory exchange models for primary matched execution
            BigDecimal notional = newLtp.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            String ordId = "ORD-" + (System.currentTimeMillis() % 1000000);
            String trdId = "TRD-" + (System.currentTimeMillis() % 1000000);

            OrderRecord order = new OrderRecord(
                    ordId, client, Instant.now(), symbol, side, orderType,
                    quantity, quantity, newLtp, newLtp, "Filled", "DAY", notional
            );

            TradeRecord trade = new TradeRecord(
                    trdId, ordId, Instant.now(), symbol, side, quantity, newLtp, notional
            );

            exchangeService.recordTickExecution(symbol, newLtp, newBid, newAsk, quantity, notional, order, trade);
            tradesCount.incrementAndGet();

            // 6. Simultaneously fluctuate a batch of 3-5 random stocks to keep the entire directory active
            List<StockRecord> samplePool = exchangeService.getStocks("All", "");
            if (!samplePool.isEmpty()) {
                int batchSize = 3 + random.nextInt(3);
                for (int b = 0; b < batchSize; b++) {
                    StockRecord rStock = samplePool.get(random.nextInt(samplePool.size()));
                    if (!rStock.symbol().equalsIgnoreCase(symbol)) {
                        boolean bgBuy = random.nextDouble() < currentRegime.getBuyProbability();
                        BigDecimal[] bgQuotes = calculateRegimePrice(rStock.symbol(), rStock.lastTradedPrice(), new BigDecimal("0.05"), bgBuy);
                        long bgQty = 25 + random.nextInt(8) * 25L;
                        BigDecimal bgNotional = bgQuotes[0].multiply(BigDecimal.valueOf(bgQty)).setScale(2, RoundingMode.HALF_UP);
                        exchangeService.updateStockFluctuation(rStock.symbol(), bgQuotes[0], bgQuotes[1], bgQuotes[2], bgQty, bgNotional);
                    }
                }
            }

            // 7. Periodic logging and PostgreSQL synchronization
            if (currentTick % 10 == 0) {
                if (currentRegime == MarketRegime.CRASH && random.nextDouble() < 0.35) {
                    exchangeService.recordMarketEvent(String.format("CIRCUIT WARNING: %s executing at ₹ %s with spread ₹ %s (Panic Sell Flow)",
                            symbol, newLtp, newAsk.subtract(newBid)));
                } else if (currentRegime == MarketRegime.BULL && isBuy && random.nextDouble() < 0.25) {
                    exchangeService.recordMarketEvent(String.format("BUYING MOMENTUM: %s executed %d shares @ ₹ %s (₹ %s)",
                            symbol, quantity, newLtp, notional));
                } else if (currentRegime == MarketRegime.BEAR && !isBuy && random.nextDouble() < 0.25) {
                    exchangeService.recordMarketEvent(String.format("SELLING PRESSURE: %s executed %d shares @ ₹ %s (Bearish Flow)",
                            symbol, quantity, newLtp));
                }

                notifyDataChanged();
            }

            // 8. Sync to PostgreSQL if configured
            if (dataSource != null && currentTick % 5 == 0) {
                syncToPostgresAsync(symbol, newLtp, newBid, newAsk, quantity, notional, ordId, trdId, side, orderType, client);
            }

        } catch (Exception ex) {
            log.debug("Error during exchange simulation tick: {}", ex.getMessage());
        }
    }

    private void syncToPostgresAsync(String symbol, BigDecimal ltp, BigDecimal bid, BigDecimal ask,
                                     long quantity, BigDecimal notional, String ordId, String trdId,
                                     String side, String orderType, String client) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Update exchange.stocks
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE exchange.stocks SET ltp = ?, bid_price = ?, ask_price = ?, volume = volume + ?, turnover = turnover + ? WHERE symbol = ?")) {
                    ps.setBigDecimal(1, ltp);
                    ps.setBigDecimal(2, bid);
                    ps.setBigDecimal(3, ask);
                    ps.setLong(4, quantity);
                    ps.setBigDecimal(5, notional);
                    ps.setString(6, symbol);
                    ps.executeUpdate();
                }

                // Insert into exchange.orders
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO exchange.orders (order_id, client_ref_id, symbol, side, order_type, quantity, filled_quantity, limit_price, average_fill_price, status, time_in_force, estimated_notional_inr) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Filled', 'DAY', ?) ON CONFLICT (order_id) DO NOTHING")) {
                    ps.setString(1, ordId);
                    ps.setString(2, client);
                    ps.setString(3, symbol);
                    ps.setString(4, side);
                    ps.setString(5, orderType);
                    ps.setLong(6, quantity);
                    ps.setLong(7, quantity);
                    ps.setBigDecimal(8, ltp);
                    ps.setBigDecimal(9, ltp);
                    ps.setBigDecimal(10, notional);
                    ps.executeUpdate();
                }

                // Insert into exchange.trades
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO exchange.trades (trade_id, order_id, symbol, side, quantity, price, gross_value_inr) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (trade_id) DO NOTHING")) {
                    ps.setString(1, trdId);
                    ps.setString(2, ordId);
                    ps.setString(3, symbol);
                    ps.setString(4, side);
                    ps.setLong(5, quantity);
                    ps.setBigDecimal(6, ltp);
                    ps.setBigDecimal(7, notional);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                log.debug("DB sync transient error: {}", ex.getMessage());
            }
        });
    }
}
