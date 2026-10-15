package com.lro.frontend.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-Time High-Throughput Banking Transaction Simulator.
 * Simulates 5 to 10 transactions per second directly in PostgreSQL,
 * affecting customer account balances, loan principals, EMI repayments,
 * and double-entry general ledger entries.
 */
public class BankTransactionSimulator {

    private static final Logger log = LoggerFactory.getLogger(BankTransactionSimulator.class);

    private final DataSource dataSource;
    private final Random random = new Random();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong transactionCount = new AtomicLong(0);

    private ScheduledExecutorService executorService;
    private volatile int transactionsPerSecond = 35; // Increased speed (35 txns/sec)

    private static final String[] SCHEMAS = {
            "bank_large", "bank_large", "bank_large", // weighted towards large
            "bank_medium", "bank_medium",
            "bank_small",
            "bank_distressed"
    };

    private static final String[] LOAN_TYPES = {
            "Home Loan", "Mortgage Loan", "Gold Loan", "Vehicle Loan",
            "Loan Against Securities", "Personal Loan", "Education Loan",
            "Business Loan", "Credit Card"
    };

    private static final String[] TYPES = {
            "Deposits", "Withdrawal", "Transfers", "Payments", "Investment"
    };

    private static final java.util.Map<String, String[]> TYPE_TO_METHODS = java.util.Map.of(
            "Deposits", new String[]{"Cash", "Cheque", "NEFT", "RTGS", "IMPS", "UPI"},
            "Withdrawal", new String[]{"Cash", "UPI", "Cheque", "IMPS"},
            "Transfers", new String[]{"UPI", "IMPS", "NEFT", "RTGS"},
            "Payments", new String[]{"UPI", "IMPS", "NEFT", "Cheque", "Cash"},
            "Investment", new String[]{"RTGS", "NEFT", "Cheque", "IMPS"}
    );

    private String[] getRandomTypeAndMethod() {
        String type = TYPES[random.nextInt(TYPES.length)];
        String[] methods = TYPE_TO_METHODS.get(type);
        String method = methods[random.nextInt(methods.length)];
        return new String[]{type, method};
    }

    public BankTransactionSimulator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting live transaction simulator at {} txns/second...", transactionsPerSecond);
            executorService = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "Lro-Live-Txn-Simulator");
                t.setDaemon(true);
                return t;
            });

            // Schedule recurring ticks based on target frequency (e.g. ~28ms for 35 txns/sec)
            long intervalMillis = Math.max(20, 1000 / transactionsPerSecond);
            executorService.scheduleAtFixedRate(this::executeRandomTransaction, 0, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void pause() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Pausing live transaction simulator.");
            if (executorService != null) {
                executorService.shutdownNow();
            }
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

    public long getTransactionCount() {
        return transactionCount.get();
    }

    public void setTransactionsPerSecond(int tps) {
        this.transactionsPerSecond = Math.max(1, Math.min(50, tps));
        if (isRunning()) {
            pause();
            resume();
        }
    }

    public int getTransactionsPerSecond() {
        return transactionsPerSecond;
    }

    /**
     * Executes one atomic banking transaction across PostgreSQL:
     * - Credit (Deposit / Salary / Inflow)
     * - Debit (Withdrawal / UPI / Merchant purchase)
     * - EMI Payment (Deducts account balance, reduces loan principal)
     * - New Loan Origination (Sanctions new loan, credits customer account)
     * - Loan Payoff / Full Closure
     */
    private void executeRandomTransaction() {
        if (!isRunning.get() || dataSource == null) return;

        String schema = SCHEMAS[random.nextInt(SCHEMAS.length)];
        int actionRoll = random.nextInt(100);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (actionRoll < 40) {
                    // 1. Credit Transaction (Deposit / Salary / Wire in) - 40% probability
                    simulateCredit(conn, schema);
                } else if (actionRoll < 70) {
                    // 2. Debit Transaction (Withdrawal / Card / Purchase) - 30% probability
                    simulateDebit(conn, schema);
                } else if (actionRoll < 90) {
                    // 3. EMI Payment (Reduces loan principal, debits account) - 20% probability
                    simulateEmiPayment(conn, schema);
                } else if (actionRoll < 96) {
                    // 4. Buy New Loan (Origination & Disbursement) - 6% probability
                    simulateLoanOrigination(conn, schema);
                } else {
                    // 5. Pay Off Old Loan (Full Settlement / Closure) - 4% probability
                    simulateLoanPayoff(conn, schema);
                }

                conn.commit();
                transactionCount.incrementAndGet();
            } catch (Exception ex) {
                conn.rollback();
                log.debug("Simulated transaction rollback in {}: {}", schema, ex.getMessage());
            }
        } catch (SQLException ex) {
            log.warn("Database error in transaction simulator: {}", ex.getMessage());
        }
    }

    private void simulateCredit(Connection conn, String schema) throws SQLException {
        // Find random active account
        String pickSql = "SELECT account_id FROM " + schema + ".accounts ORDER BY random() LIMIT 1;";
        String accId = null;
        try (PreparedStatement ps = conn.prepareStatement(pickSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                accId = rs.getString("account_id");
            }
        }
        if (accId == null) return;

        // Credit amount between ₹2,000 and ₹1,50,000
        BigDecimal amount = BigDecimal.valueOf(2000 + random.nextInt(148000)).setScale(2, RoundingMode.HALF_UP);

        String updateSql = "UPDATE " + schema + ".accounts SET available_balance = available_balance + ? WHERE account_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, accId);
            ps.executeUpdate();
        }

        String[] tm = getRandomTypeAndMethod();
        // Insert double-entry ledger record
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random()*10000000, 'FM0000000'), 'TXN-CR-' || to_char(random()*1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'CLEARING_SETTLEMENT', ?, 0.00, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(ledgerSql)) {
            ps.setString(1, accId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, tm[0]);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }

        // Attach live transaction to running balance sheet
        String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                "VALUES ('BS-' || to_char(random()*10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'CASA Deposits', 'Liability', 0.00, ?, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) + ?, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(bsSql)) {
            ps.setString(1, accId);
            ps.setBigDecimal(2, amount);
            ps.setBigDecimal(3, amount);
            ps.setString(4, tm[0]);
            ps.setString(5, tm[1]);
            ps.executeUpdate();
        }
    }

    private void simulateDebit(Connection conn, String schema) throws SQLException {
        // Pick an account with positive balance
        String pickSql = "SELECT account_id, available_balance FROM " + schema + ".accounts WHERE available_balance > 5000 ORDER BY random() LIMIT 1;";
        String accId = null;
        BigDecimal bal = null;
        try (PreparedStatement ps = conn.prepareStatement(pickSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                accId = rs.getString("account_id");
                bal = rs.getBigDecimal("available_balance");
            }
        }
        if (accId == null || bal == null) return;

        // Debit between ₹500 and min(₹40,000, bal * 0.4)
        int maxDebit = Math.min(40000, bal.multiply(BigDecimal.valueOf(0.4)).intValue());
        if (maxDebit < 500) return;
        BigDecimal amount = BigDecimal.valueOf(500 + random.nextInt(maxDebit)).setScale(2, RoundingMode.HALF_UP);

        String updateSql = "UPDATE " + schema + ".accounts SET available_balance = available_balance - ? WHERE account_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, accId);
            ps.executeUpdate();
        }

        String[] tm = getRandomTypeAndMethod();
        // Insert double-entry ledger entry
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random()*10000000, 'FM0000000'), 'TXN-DR-' || to_char(random()*1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'MERCHANT_OUTFLOW', 0.00, ?, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(ledgerSql)) {
            ps.setString(1, accId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, tm[0]);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }

        // Attach live transaction to running balance sheet
        String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                "VALUES ('BS-' || to_char(random()*10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'CASA Deposits', 'Liability', ?, 0.00, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) - ?, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(bsSql)) {
            ps.setString(1, accId);
            ps.setBigDecimal(2, amount);
            ps.setBigDecimal(3, amount);
            ps.setString(4, tm[0]);
            ps.setString(5, tm[1]);
            ps.executeUpdate();
        }
    }

    private void simulateEmiPayment(Connection conn, String schema) throws SQLException {
        // Pick active loan with positive principal
        String pickLoan = "SELECT loan_id, customer_id, principal_outstanding, monthly_emi, days_past_due FROM " + schema + ".loans WHERE principal_outstanding > 0 ORDER BY random() LIMIT 1;";
        String loanId = null;
        String custId = null;
        BigDecimal principal = null;
        BigDecimal emi = null;
        int dpd = 0;

        try (PreparedStatement ps = conn.prepareStatement(pickLoan);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                loanId = rs.getString("loan_id");
                custId = rs.getString("customer_id");
                principal = rs.getBigDecimal("principal_outstanding");
                emi = rs.getBigDecimal("monthly_emi");
                dpd = rs.getInt("days_past_due");
            }
        }
        if (loanId == null || custId == null || emi == null) return;

        // Reduce loan principal outstanding by EMI principal component (approx 80% of EMI)
        BigDecimal principalPortion = emi.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        if (principalPortion.compareTo(principal) > 0) {
            principalPortion = principal;
        }

        int newDpd = Math.max(0, dpd - 30);
        String newIrac = newDpd > 90 ? "Sub-Standard NPA" : newDpd > 60 ? "SMA-2" : newDpd > 30 ? "SMA-1" : newDpd > 0 ? "SMA-0" : "Standard";

        String updateLoanSql = "UPDATE " + schema + ".loans SET principal_outstanding = principal_outstanding - ?, days_past_due = ?, irac_classification = ?, next_emi_due_date = CURRENT_DATE + interval '30 day' WHERE loan_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(updateLoanSql)) {
            ps.setBigDecimal(1, principalPortion);
            ps.setInt(2, newDpd);
            ps.setString(3, newIrac);
            ps.setString(4, loanId);
            ps.executeUpdate();
        }

        // Debit customer's operational account
        String debitAccSql = "UPDATE " + schema + ".accounts SET available_balance = available_balance - ? WHERE customer_id = ? AND account_type IN ('Saving', 'Salary', 'Current') AND available_balance >= ? LIMIT 1;";
        try (PreparedStatement ps = conn.prepareStatement(debitAccSql)) {
            ps.setBigDecimal(1, emi);
            ps.setString(2, custId);
            ps.setBigDecimal(3, emi);
            ps.executeUpdate();
        }

        String[] tm = getRandomTypeAndMethod();
        // Record in General Ledger
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random()*10000000, 'FM0000000'), 'TXN-EMI-' || to_char(random()*1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'LOAN_PORTFOLIO_ASSET', 0.00, ?, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(ledgerSql)) {
            ps.setString(1, custId);
            ps.setBigDecimal(2, emi);
            ps.setString(3, tm[0]);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }

        // Attach live transaction to running balance sheet
        String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                "VALUES ('BS-' || to_char(random()*10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'Loans & Advances', 'Asset', 0.00, ?, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) - ?, 'EMI Repayment', ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(bsSql)) {
            ps.setString(1, custId);
            ps.setBigDecimal(2, principalPortion);
            ps.setBigDecimal(3, principalPortion);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }
    }

    private void simulateLoanOrigination(Connection conn, String schema) throws SQLException {
        // Select random customer
        String pickCust = "SELECT customer_id FROM " + schema + ".customers ORDER BY random() LIMIT 1;";
        String custId = null;
        try (PreparedStatement ps = conn.prepareStatement(pickCust);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                custId = rs.getString("customer_id");
            }
        }
        if (custId == null) return;

        String loanType = LOAN_TYPES[random.nextInt(LOAN_TYPES.length)];
        String prefix = "bank_large".equals(schema) ? "LN-LRG" : "bank_medium".equals(schema) ? "LN-MED" : "bank_small".equals(schema) ? "LN-SML" : "LN-DST";
        String loanId = prefix + "-" + String.format("%05d", 12000 + random.nextInt(88000));

        BigDecimal sanctionedAmount = BigDecimal.valueOf(100000 + random.nextInt(4900000)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal interestRate = BigDecimal.valueOf(8.50 + (random.nextInt(600) / 100.0)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal emi = sanctionedAmount.multiply(BigDecimal.valueOf(0.015)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal collateral = sanctionedAmount.multiply(BigDecimal.valueOf(1.25)).setScale(2, RoundingMode.HALF_UP);

        String insertLoan = "INSERT INTO " + schema + ".loans (loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding, interest_rate_pct, days_past_due, irac_classification, collateral_value, next_emi_due_date, monthly_emi) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0, 'Standard', ?, CURRENT_DATE + interval '30 day', ?) ON CONFLICT DO NOTHING;";
        try (PreparedStatement ps = conn.prepareStatement(insertLoan)) {
            ps.setString(1, loanId);
            ps.setString(2, custId);
            ps.setString(3, loanType);
            ps.setBigDecimal(4, sanctionedAmount);
            ps.setBigDecimal(5, sanctionedAmount);
            ps.setBigDecimal(6, interestRate);
            ps.setBigDecimal(7, collateral);
            ps.setBigDecimal(8, emi);
            ps.executeUpdate();
        }

        // Credit customer account with loan proceeds
        String creditAcc = "UPDATE " + schema + ".accounts SET available_balance = available_balance + ? WHERE customer_id = ? AND account_type IN ('Saving', 'Current') LIMIT 1;";
        try (PreparedStatement ps = conn.prepareStatement(creditAcc)) {
            ps.setBigDecimal(1, sanctionedAmount);
            ps.setString(2, custId);
            ps.executeUpdate();
        }

        String[] tm = getRandomTypeAndMethod();
        // Insert double-entry ledger
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random()*10000000, 'FM0000000'), 'TXN-LN-DISB-' || to_char(random()*1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'LOAN_DISBURSEMENT_PAYABLE', ?, 0.00, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(ledgerSql)) {
            ps.setString(1, custId);
            ps.setBigDecimal(2, sanctionedAmount);
            ps.setString(3, tm[0]);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }

        // Attach live transaction to running balance sheet
        String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                "VALUES ('BS-' || to_char(random()*10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'Loans & Advances', 'Asset', ?, 0.00, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) + ?, 'Loan Disbursement', ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(bsSql)) {
            ps.setString(1, custId);
            ps.setBigDecimal(2, sanctionedAmount);
            ps.setBigDecimal(3, sanctionedAmount);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }
    }

    private void simulateLoanPayoff(Connection conn, String schema) throws SQLException {
        // Find loan with moderate remaining principal
        String pickLoan = "SELECT loan_id, customer_id, principal_outstanding FROM " + schema + ".loans WHERE principal_outstanding > 0 AND principal_outstanding < 300000 ORDER BY random() LIMIT 1;";
        String loanId = null;
        String custId = null;
        BigDecimal outstanding = null;

        try (PreparedStatement ps = conn.prepareStatement(pickLoan);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                loanId = rs.getString("loan_id");
                custId = rs.getString("customer_id");
                outstanding = rs.getBigDecimal("principal_outstanding");
            }
        }
        if (loanId == null || custId == null || outstanding == null) return;

        // Mark loan paid off and close
        String closeLoan = "UPDATE " + schema + ".loans SET principal_outstanding = 0.00, days_past_due = 0, irac_classification = 'Standard' WHERE loan_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(closeLoan)) {
            ps.setString(1, loanId);
            ps.executeUpdate();
        }

        // Debit customer account
        String debitAcc = "UPDATE " + schema + ".accounts SET available_balance = GREATEST(0.00, available_balance - ?) WHERE customer_id = ? AND account_type IN ('Saving', 'Current') LIMIT 1;";
        try (PreparedStatement ps = conn.prepareStatement(debitAcc)) {
            ps.setBigDecimal(1, outstanding);
            ps.setString(2, custId);
            ps.executeUpdate();
        }

        String[] tm = getRandomTypeAndMethod();
        // Ledger settlement entry
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random()*10000000, 'FM0000000'), 'TXN-LN-CLOSE-' || to_char(random()*1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'LOAN_CLOSURE_SETTLEMENT', 0.00, ?, ?, ?, 'Posted');";
        try (PreparedStatement ps = conn.prepareStatement(ledgerSql)) {
            ps.setString(1, custId);
            ps.setBigDecimal(2, outstanding);
            ps.setString(3, tm[0]);
            ps.setString(4, tm[1]);
            ps.executeUpdate();
        }
    }
}
