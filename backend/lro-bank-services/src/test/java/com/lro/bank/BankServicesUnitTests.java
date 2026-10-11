package com.lro.bank;

import com.lro.bank.service.BankPopulationGenerator;
import com.lro.bank.service.BankPopulationGenerator.SyntheticBankPopulation;
import com.lro.bank.service.DoubleEntryLedgerService;
import com.lro.bank.service.DoubleEntryLedgerService.JournalEntry;
import com.lro.bank.service.LoanNpaService;
import com.lro.bank.service.LoanNpaService.IracClassification;
import com.lro.bank.service.TreasuryLadderService;
import com.lro.bank.service.TreasuryLadderService.MaturityBucket;
import com.lro.common.exception.ValidationException;
import com.lro.common.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BankServicesUnitTests {

    @Test
    @DisplayName("Verify Prompt 13: Deterministic population targets across 4 banks")
    void testPopulationGeneration() {
        SyntheticBankPopulation large = BankPopulationGenerator.generatePopulation("BANK-01-LRG", 42L);
        assertEquals(10000L, large.customerCount());
        assertTrue(large.loanCount() >= 1000L);
        assertTrue(large.treasuryLotsCount() >= 500L);
        assertEquals(new BigDecimal("1.25"), large.grossNpaRatio());

        SyntheticBankPopulation distressed = BankPopulationGenerator.generatePopulation("BANK-04-DST", 42L);
        assertEquals(5000L, distressed.customerCount());
        assertEquals(new BigDecimal("14.80"), distressed.grossNpaRatio());
    }

    @Test
    @DisplayName("Verify Prompt 14: Double-entry ledger enforces debit equals credit invariant")
    void testDoubleEntryLedger() {
        DoubleEntryLedgerService ledger = new DoubleEntryLedgerService();

        // Balanced transaction
        List<JournalEntry> balanced = List.of(
                new JournalEntry("E1", "TXN-01", Instant.now(), "CASH", Money.of("50000.00"), Money.ZERO, "Deposit Inflow", "Posted"),
                new JournalEntry("E2", "TXN-01", Instant.now(), "SAVINGS_LIAB", Money.ZERO, Money.of("50000.00"), "Deposit Inflow", "Posted")
        );

        assertDoesNotThrow(() -> ledger.postTransaction("BANK-01-LRG", "TXN-01", "Cash Deposit", balanced));
        assertEquals(2, ledger.getLedgerEntries("BANK-01-LRG").size());

        // Unbalanced transaction must throw ValidationException
        List<JournalEntry> unbalanced = List.of(
                new JournalEntry("E3", "TXN-02", Instant.now(), "CASH", Money.of("50000.00"), Money.ZERO, "Bad Post", "Pending"),
                new JournalEntry("E4", "TXN-02", Instant.now(), "SAVINGS_LIAB", Money.ZERO, Money.of("40000.00"), "Bad Post", "Pending")
        );

        assertThrows(ValidationException.class, () -> ledger.postTransaction("BANK-01-LRG", "TXN-02", "Unbalanced", unbalanced));
    }

    @Test
    @DisplayName("Verify Prompt 14: Loan EMI calculation and IRAC NPA transitions")
    void testLoanNpaRules() {
        Money emi = LoanNpaService.calculateMonthlyEmi(Money.of("1000000.00"), new BigDecimal("8.50"), 120);
        assertTrue(emi.getAmount().doubleValue() > 12000.0);

        assertEquals(IracClassification.STANDARD, LoanNpaService.classifyLoan(0));
        assertEquals(IracClassification.SMA_0, LoanNpaService.classifyLoan(15));
        assertEquals(IracClassification.SMA_1, LoanNpaService.classifyLoan(45));
        assertEquals(IracClassification.SMA_2, LoanNpaService.classifyLoan(75));
        assertEquals(IracClassification.SUB_STANDARD_NPA, LoanNpaService.classifyLoan(95));
        assertTrue(LoanNpaService.isNpa(95));
        assertFalse(LoanNpaService.isNpa(45));
    }

    @Test
    @DisplayName("Verify Prompt 15: Cash-flow maturity ladder calculation")
    void testTreasuryLadder() {
        List<MaturityBucket> ladder = TreasuryLadderService.buildLadder(
                Money.of("100.00"), Money.of("150.00"), // 1D
                Money.of("200.00"), Money.of("180.00"), // 2-7D
                Money.of("300.00"), Money.of("250.00"), // 8-30D
                Money.of("400.00"), Money.of("350.00"), // 31-90D
                Money.of("1000.00"), Money.of("900.00") // >90D
        );

        assertEquals(5, ladder.size());
        assertEquals(Money.of("-50.00"), ladder.get(0).netGap());
        assertEquals(Money.of("-50.00"), ladder.get(0).cumulativeGap());
        assertEquals(Money.of("20.00"), ladder.get(1).netGap());
        assertEquals(Money.of("-30.00"), ladder.get(1).cumulativeGap());
    }
}
