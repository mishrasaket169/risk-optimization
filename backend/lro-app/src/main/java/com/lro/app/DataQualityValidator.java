package com.lro.app;

import com.lro.bank.service.DoubleEntryLedgerService;
import com.lro.common.exception.ValidationException;
import com.lro.common.model.Money;

import java.util.Map;

/**
 * Data Quality & Ledger Sanity Engine implementing Prompt 25.
 * Verifies core accounting invariants and data integrity.
 */
public class DataQualityValidator {

    public record QualityReport(
            boolean passed,
            int totalChecksRun,
            int passedCount,
            int failedCount,
            String details
    ) {}

    public static QualityReport validateLedgerInvariance(DoubleEntryLedgerService ledgerService, String bankId) {
        Map<String, Money> trialBalance = ledgerService.getTrialBalance(bankId);
        Money netImbalance = Money.ZERO;

        for (Money balance : trialBalance.values()) {
            netImbalance = netImbalance.add(balance);
        }

        boolean balanced = netImbalance.isZero();
        if (!balanced) {
            return new QualityReport(false, 1, 0, 1, "Trial balance imbalance detected: " + netImbalance);
        }
        return new QualityReport(true, 1, 1, 0, "Trial balance strictly balanced. Sum of debits equals credits.");
    }
}
