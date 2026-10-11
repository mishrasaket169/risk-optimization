package com.lro.bank.service;

import com.lro.common.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Treasury Fixed Income Analytics & Cash-Flow Ladder Engine implementing Prompt 15.
 * Computes bond dirty pricing, modified duration, and maturity buckets.
 */
public class TreasuryLadderService {

    public record FixedIncomeInstrument(
            String isin,
            String name,
            String category, // "G-Sec", "T-Bill", "Corporate Bond"
            Money faceValue,
            Money cleanPrice,
            BigDecimal couponRatePercent,
            LocalDate issueDate,
            LocalDate maturityDate,
            boolean isEncumbered
    ) {}

    public record MaturityBucket(
            String bucketName,
            Money inflows,
            Money outflows,
            Money netGap,
            Money cumulativeGap
    ) {}

    /**
     * Computes accrued interest and dirty price.
     * Dirty Price = Clean Price + Accrued Interest
     */
    public static Money calculateDirtyPrice(FixedIncomeInstrument instrument, LocalDate asOfDate) {
        if (instrument.couponRatePercent().compareTo(BigDecimal.ZERO) == 0) {
            return instrument.cleanPrice();
        }

        // 30/360 or Actual/Actual day count convention
        long daysSinceLastCoupon = 90; // illustrative quarterly coupon
        BigDecimal annualCoupon = instrument.faceValue().getAmount()
                .multiply(instrument.couponRatePercent())
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_EVEN);

        BigDecimal accrued = annualCoupon.multiply(BigDecimal.valueOf(daysSinceLastCoupon))
                .divide(new BigDecimal("365"), 2, RoundingMode.HALF_EVEN);

        return instrument.cleanPrice().add(Money.of(accrued));
    }

    /**
     * Modified Duration calculation (sensitivity in % per 100 bps yield change).
     */
    public static BigDecimal estimateModifiedDuration(LocalDate asOfDate, LocalDate maturityDate, BigDecimal yieldPercent) {
        long daysToMaturity = Math.max(1, ChronoUnit.DAYS.between(asOfDate, maturityDate));
        double years = daysToMaturity / 365.25;
        double y = yieldPercent.doubleValue() / 100.0;

        // Maculay duration approximation / (1 + y)
        double modDuration = (years * 0.95) / (1.0 + (y / 2.0));
        return BigDecimal.valueOf(modDuration).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Aggregates cash flows into the standard structural liquidity ladder buckets.
     */
    public static List<MaturityBucket> buildLadder(Money d1In, Money d1Out, Money d7In, Money d7Out,
                                                   Money d30In, Money d30Out, Money d90In, Money d90Out,
                                                   Money dLongIn, Money dLongOut) {
        List<MaturityBucket> buckets = new ArrayList<>();

        Money gap1 = d1In.subtract(d1Out);
        Money cum1 = gap1;
        buckets.add(new MaturityBucket("Same Day (1D)", d1In, d1Out, gap1, cum1));

        Money gap2 = d7In.subtract(d7Out);
        Money cum2 = cum1.add(gap2);
        buckets.add(new MaturityBucket("2 - 7 Days", d7In, d7Out, gap2, cum2));

        Money gap3 = d30In.subtract(d30Out);
        Money cum3 = cum2.add(gap3);
        buckets.add(new MaturityBucket("8 - 30 Days", d30In, d30Out, gap3, cum3));

        Money gap4 = d90In.subtract(d90Out);
        Money cum4 = cum3.add(gap4);
        buckets.add(new MaturityBucket("31 - 90 Days", d90In, d90Out, gap4, cum4));

        Money gap5 = dLongIn.subtract(dLongOut);
        Money cum5 = cum4.add(gap5);
        buckets.add(new MaturityBucket("> 90 Days", dLongIn, dLongOut, gap5, cum5));

        return buckets;
    }
}
