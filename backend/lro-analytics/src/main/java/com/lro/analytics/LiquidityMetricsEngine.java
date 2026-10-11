package com.lro.analytics;

import com.lro.common.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Liquidity Metrics & Risk Analytics Engine implementing Prompt 20:
 * Computes Basel III HQLA categories, regulatory haircuts, educational LCR/NSFR,
 * and 99% Parametric Value-at-Risk (VaR).
 */
public class LiquidityMetricsEngine {

    public enum HqlaCategory {
        LEVEL_1,  // Cash, Sovereign G-Secs, T-Bills (0% - 0.5% haircut)
        LEVEL_2A, // AAA Corporate debt, Public sector bonds (15% haircut)
        LEVEL_2B, // High-grade equities (50% haircut)
        NON_HQLA  // Unlisted or illiquid holdings
    }

    public static BigDecimal getRegulatoryHaircut(HqlaCategory category) {
        return switch (category) {
            case LEVEL_1 -> new BigDecimal("0.50");   // 0.5% standard sovereign buffer haircut
            case LEVEL_2A -> new BigDecimal("15.00"); // 15% haircut
            case LEVEL_2B -> new BigDecimal("50.00"); // 50% haircut
            case NON_HQLA -> new BigDecimal("100.00");// 100% deduction
        };
    }

    public static Money calculateLiquidationProceeds(Money marketValue, BigDecimal haircutPercent) {
        BigDecimal haircutFactor = BigDecimal.ONE.subtract(
                haircutPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_EVEN)
        );
        return marketValue.multiply(haircutFactor);
    }

    /**
     * Educational Liquidity Coverage Ratio (LCR):
     * LCR = (Total Eligible HQLA after Haircuts) / (Total Net Stressed 30D Outflows) × 100%
     */
    public static BigDecimal calculateEducationalLcr(Money totalEligibleHqla, Money netStressed30dOutflows) {
        if (netStressed30dOutflows.isZero() || netStressed30dOutflows.isNegative()) {
            return new BigDecimal("999.99"); // Infinite buffer when no net outflows
        }

        BigDecimal hqla = totalEligibleHqla.getAmount();
        BigDecimal outflows = netStressed30dOutflows.getAmount();

        return hqla.divide(outflows, 4, RoundingMode.HALF_EVEN)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Parametric 1-Day Value-at-Risk (VaR) at 99% Confidence Level:
     * VaR_99 = 2.3263 × Portfolio Value × Daily Volatility
     */
    public static Money calculateParametricVaR99(Money portfolioMarketValue, BigDecimal annualizedVolatility) {
        // Daily volatility = Annualized / sqrt(252)
        double dailyVol = annualizedVolatility.doubleValue() / Math.sqrt(252.0);
        double z99 = 2.3263;

        double varAmount = z99 * portfolioMarketValue.getAmount().doubleValue() * dailyVol;
        return Money.of(new BigDecimal(varAmount).setScale(2, RoundingMode.HALF_EVEN));
    }

    public Money calculateTotalHqla(Money cash, Money gsec, Money corpBond, Money equities) {
        Money l1Cash = cash; // Level 1 (0% haircut)
        Money l1Gsec = gsec; // Level 1 (0% haircut)
        Money l2a = calculateLiquidationProceeds(corpBond, new BigDecimal("15.00")); // Level 2A (15% haircut)
        Money l2b = calculateLiquidationProceeds(equities, new BigDecimal("50.00")); // Level 2B (50% haircut)
        return l1Cash.add(l1Gsec).add(l2a).add(l2b);
    }

    public double calculateIllustrativeLcr(Money totalEligibleHqla, Money netOutflows) {
        return calculateEducationalLcr(totalEligibleHqla, netOutflows).doubleValue();
    }

    public Money calculateParametricVaR(Money portfolioValue, double dailyVol, double zScore) {
        double varAmt = portfolioValue.getAmount().doubleValue() * dailyVol * zScore;
        return Money.of(new BigDecimal(varAmt).setScale(2, RoundingMode.HALF_EVEN));
    }
}
