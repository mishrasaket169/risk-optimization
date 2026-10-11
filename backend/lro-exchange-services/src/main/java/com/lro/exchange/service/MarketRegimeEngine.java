package com.lro.exchange.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Market Regime Engine implementing Prompt 17:
 * Coordinates simulated market dynamics across Normal, Bull, Bear, and Crash modes.
 */
public class MarketRegimeEngine {

    public enum Regime {
        NORMAL,
        BULL,
        BEAR,
        CRASH
    }

    private Regime currentRegime = Regime.NORMAL;
    private int simulationSpeed = 1;
    private long randomSeed = 42L;

    public void setRegime(Regime regime) {
        this.currentRegime = regime;
    }

    public Regime getRegime() {
        return currentRegime;
    }

    public void setParameters(int speed, long seed) {
        this.simulationSpeed = Math.max(1, Math.min(10, speed));
        this.randomSeed = seed;
    }

    public int getSimulationSpeed() {
        return simulationSpeed;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Spread widening factor applied to order book quotes during stress.
     */
    public BigDecimal getSpreadMultiplier() {
        return switch (currentRegime) {
            case NORMAL -> BigDecimal.ONE;
            case BULL -> new BigDecimal("0.85");
            case BEAR -> new BigDecimal("1.60");
            case CRASH -> new BigDecimal("3.80"); // Severe illiquidity
        };
    }

    /**
     * Volatility drift multiplier.
     */
    public BigDecimal getVolatilityFactor() {
        return switch (currentRegime) {
            case NORMAL -> BigDecimal.ONE;
            case BULL -> new BigDecimal("1.20");
            case BEAR -> new BigDecimal("1.80");
            case CRASH -> new BigDecimal("4.50");
        };
    }
}
