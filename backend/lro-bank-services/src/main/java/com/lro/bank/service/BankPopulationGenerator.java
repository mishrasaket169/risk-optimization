package com.lro.bank.service;

import com.lro.common.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Deterministic Synthetic Data Generator implementing Prompt 13:
 * Generates populations for the 4 banks:
 * - Large Healthy Bank: >= 10,000 customers, >= 1,000 loans, ~500 G-Sec lots, low NPA (<2.5%)
 * - Medium Bank: >= 7,500 customers
 * - Small Healthy Bank: >= 5,000 customers
 * - Distressed Bank: >= 5,000 customers, high NPA share (>14%) and strained liquidity
 */
public class BankPopulationGenerator {

    public record SyntheticBankPopulation(
            String bankId,
            String bankName,
            long customerCount,
            long loanCount,
            long treasuryLotsCount,
            BigDecimal grossNpaRatio,
            List<String> customerIds,
            Money totalDeposits,
            Money totalLoans,
            Money cashReserve
    ) {}

    public static SyntheticBankPopulation generatePopulation(String bankId, long seed) {
        Random random = new Random(seed);

        long targetCustomers;
        long targetLoans;
        long targetTreasuryLots;
        BigDecimal npaRatio;
        Money totalDeposits;
        Money totalLoans;
        Money cashReserve;
        String bankName;

        switch (bankId.toUpperCase()) {
            case "BANK-01-LRG", "BANK_LARGE" -> {
                bankName = "Apex National Bank (Synthetic)";
                targetCustomers = 10000L;
                targetLoans = 1200L;
                targetTreasuryLots = 500L;
                npaRatio = new BigDecimal("1.25");
                totalDeposits = Money.of(new BigDecimal("390000000000.00")); // ₹ 39,000 Cr
                totalLoans = Money.of(new BigDecimal("312000000000.00"));    // ₹ 31,200 Cr
                cashReserve = Money.of(new BigDecimal("42000000000.00"));    // ₹ 4,200 Cr
            }
            case "BANK-02-MED", "BANK_MEDIUM" -> {
                bankName = "Bharat Mercantile Bank (Synthetic)";
                targetCustomers = 7500L;
                targetLoans = 850L;
                targetTreasuryLots = 320L;
                npaRatio = new BigDecimal("2.40");
                totalDeposits = Money.of(new BigDecimal("162000000000.00")); // ₹ 16,200 Cr
                totalLoans = Money.of(new BigDecimal("128000000000.00"));
                cashReserve = Money.of(new BigDecimal("16500000000.00"));
            }
            case "BANK-03-SML", "BANK_SMALL" -> {
                bankName = "Sahyadri Urban Co-operative Bank (Synthetic)";
                targetCustomers = 5000L;
                targetLoans = 550L;
                targetTreasuryLots = 180L;
                npaRatio = new BigDecimal("1.80");
                totalDeposits = Money.of(new BigDecimal("64000000000.00"));  // ₹ 6,400 Cr
                totalLoans = Money.of(new BigDecimal("49500000000.00"));
                cashReserve = Money.of(new BigDecimal("7200000000.00"));
            }
            default -> { // Distressed
                bankName = "Kalyan Industrial Credit Bank (Synthetic)";
                targetCustomers = 5000L;
                targetLoans = 620L;
                targetTreasuryLots = 140L;
                npaRatio = new BigDecimal("14.80"); // High NPA
                totalDeposits = Money.of(new BigDecimal("59000000000.00"));  // Depleted deposits
                totalLoans = Money.of(new BigDecimal("58000000000.00"));
                cashReserve = Money.of(new BigDecimal("3100000000.00"));    // Thin cash buffer
            }
        }

        List<String> customerIds = new ArrayList<>();
        for (int i = 1; i <= Math.min(targetCustomers, 100); i++) {
            customerIds.add(bankId + "-CUST-" + (1000 + i));
        }

        return new SyntheticBankPopulation(
                bankId, bankName, targetCustomers, targetLoans, targetTreasuryLots,
                npaRatio, customerIds, totalDeposits, totalLoans, cashReserve
        );
    }
}
