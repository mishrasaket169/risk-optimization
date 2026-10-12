package com.lro.frontend;

import com.lro.frontend.bank.model.BankModels.*;
import com.lro.frontend.bank.service.BankTransactionSimulator;
import com.lro.frontend.bank.service.PostgresBankService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class PostgresBankServiceIntegrationTest {

    private static PostgresBankService pgService;

    @BeforeAll
    static void setUp() {
        try {
            pgService = new PostgresBankService();
        } catch (Exception e) {
            System.err.println("Could not initialize PostgresBankService: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (pgService != null) {
            pgService.close();
        }
    }

    @Test
    @DisplayName("Verify PostgreSQL database connection and schema access")
    void testConnectionAndProfiles() {
        if (pgService == null || !pgService.isConnected()) {
            System.out.println("PostgreSQL not accessible in test environment, skipping live test.");
            return;
        }

        List<BankProfile> profiles = pgService.getAllBankProfiles();
        assertEquals(4, profiles.size(), "All 4 banks (large, medium, small, distressed) should be mapped");

        for (BankProfile p : profiles) {
            assertNotNull(p.bankId());
            assertNotNull(p.bankName());
            assertTrue(p.customerCount() > 0, "Bank should have customers in DB");
            assertTrue(p.totalDeposits().compareTo(BigDecimal.ZERO) > 0, "Bank should have deposits in DB");
        }
    }

    @Test
    @DisplayName("Verify queries against PostgreSQL live accounts, loans, treasury and ledger")
    void testLiveQueries() {
        if (pgService == null || !pgService.isConnected()) {
            return;
        }

        String bankId = "BANK-01-LRG";

        List<AccountRecord> accounts = pgService.getAccounts(bankId, "", "All");
        assertFalse(accounts.isEmpty(), "Should retrieve accounts from bank_large schema");

        List<LoanRecord> loans = pgService.getLoans(bankId, "", "All");
        assertFalse(loans.isEmpty(), "Should retrieve loans from bank_large schema");

        List<TreasuryHolding> treasury = pgService.getTreasuryHoldings(bankId, "", "All");
        assertFalse(treasury.isEmpty(), "Should retrieve treasury holdings from bank_large schema");

        List<LedgerEntry> ledger = pgService.getLedgerEntries(bankId);
        assertNotNull(ledger);
    }

    @Test
    @DisplayName("Verify CDC LISTEN/NOTIFY and BankTransactionSimulator execution")
    void testSimulatorAndCdcNotification() throws InterruptedException {
        if (pgService == null || !pgService.isConnected()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean notificationReceived = new AtomicBoolean(false);

        pgService.addDataChangeListener(() -> {
            notificationReceived.set(true);
            latch.countDown();
        });

        BankTransactionSimulator simulator = new BankTransactionSimulator(pgService.getDataSource());
        simulator.setTransactionsPerSecond(10);
        simulator.start();

        // Wait up to 5 seconds for simulator to fire and CDC notification to be received
        boolean fired = latch.await(5, TimeUnit.SECONDS);
        simulator.stop();

        assertTrue(simulator.getTransactionCount() > 0, "Simulator should have executed transactions");
        assertTrue(fired && notificationReceived.get(), "PostgreSQL CDC LISTEN/NOTIFY should have triggered dataChangeListener");
    }
}
