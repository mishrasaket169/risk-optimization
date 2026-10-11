package com.lro.bank.service;

import com.lro.common.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Loan Servicing & IRAC Prudential NPA Engine implementing Prompt 14.
 * Computes monthly EMI schedules, Days Past Due (DPD), and NPA classifications under RBI rules.
 */
public class LoanNpaService {

    public enum IracClassification {
        STANDARD,
        SMA_0,
        SMA_1,
        SMA_2,
        SUB_STANDARD_NPA,
        DOUBTFUL_NPA,
        LOSS
    }

    public static IracClassification classifyLoan(int daysPastDue) {
        if (daysPastDue <= 0) return IracClassification.STANDARD;
        if (daysPastDue <= 30) return IracClassification.SMA_0;
        if (daysPastDue <= 60) return IracClassification.SMA_1;
        if (daysPastDue <= 90) return IracClassification.SMA_2;
        if (daysPastDue <= 180) return IracClassification.SUB_STANDARD_NPA;
        return IracClassification.DOUBTFUL_NPA;
    }

    public static boolean isNpa(int daysPastDue) {
        return daysPastDue > 90;
    }

    /**
     * Standard Equated Monthly Installment (EMI) formula:
     * EMI = [P × r × (1 + r)^n] / [(1 + r)^n - 1]
     */
    public static Money calculateMonthlyEmi(Money principal, BigDecimal annualRatePercent, int tenorMonths) {
        if (principal.isZero() || tenorMonths <= 0) return Money.ZERO;

        BigDecimal monthlyRate = annualRatePercent.divide(new BigDecimal("1200"), 8, RoundingMode.HALF_EVEN);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenorMonths));
        }

        double r = monthlyRate.doubleValue();
        double p = principal.getAmount().doubleValue();
        double onePlusRPowerN = Math.pow(1.0 + r, tenorMonths);

        double emiAmount = (p * r * onePlusRPowerN) / (onePlusRPowerN - 1.0);
        return Money.of(new BigDecimal(emiAmount).setScale(2, RoundingMode.HALF_EVEN));
    }

    /**
     * Required provisioning share based on RBI IRAC norms.
     */
    public static BigDecimal getRequiredProvisionRate(IracClassification classification, boolean secured) {
        return switch (classification) {
            case STANDARD -> new BigDecimal("0.40"); // 0.40% general standard provision
            case SMA_0, SMA_1, SMA_2 -> new BigDecimal("5.00");
            case SUB_STANDARD_NPA -> secured ? new BigDecimal("15.00") : new BigDecimal("25.00");
            case DOUBTFUL_NPA -> secured ? new BigDecimal("40.00") : new BigDecimal("100.00");
            case LOSS -> new BigDecimal("100.00");
        };
    }
}
