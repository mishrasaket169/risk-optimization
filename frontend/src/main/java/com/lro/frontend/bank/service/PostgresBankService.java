package com.lro.frontend.bank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lro.frontend.bank.model.BankModels.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Authoritative Live PostgreSQL Bank Service.
 * Connects directly to multi-schema PostgreSQL via HikariCP connection pool,
 * providing real-time bidirectional synchronization with PostgreSQL LISTEN/NOTIFY.
 */
public class PostgresBankService implements BankService, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PostgresBankService.class);

    private final HikariDataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Runnable> dataChangeListeners = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = true;
    private Thread notificationListenerThread;

    public PostgresBankService() {
        this(
                System.getenv().getOrDefault("LRO_DB_URL", "jdbc:postgresql://localhost:5432/lro_db"),
                System.getenv().getOrDefault("LRO_DB_USER", "postgres"),
                System.getenv().getOrDefault("LRO_DB_PASSWORD", "Saket@169")
        );
    }

    public PostgresBankService(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(3);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setPoolName("LroBankPool");

        this.dataSource = new HikariDataSource(config);

        // Start real-time background CDC listener using PostgreSQL LISTEN/NOTIFY
        startNotificationListener(jdbcUrl, username, password);
    }

    public void addDataChangeListener(Runnable listener) {
        if (listener != null) {
            dataChangeListeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : dataChangeListeners) {
            try {
                listener.run();
            } catch (Exception ex) {
                log.warn("Error running data change listener: {}", ex.getMessage());
            }
        }
    }

    private void startNotificationListener(String jdbcUrl, String username, String password) {
        notificationListenerThread = new Thread(() -> {
            log.info("Starting PostgreSQL LISTEN bank_events background thread...");
            while (isRunning) {
                try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN bank_events;");
                    }

                    while (isRunning && !conn.isClosed()) {
                        PGNotification[] notifications = pgConn.getNotifications(250); // wait up to 250ms
                        if (notifications != null && notifications.length > 0) {
                            for (PGNotification n : notifications) {
                                try {
                                    JsonNode node = objectMapper.readTree(n.getParameter());
                                    log.debug("Received PostgreSQL Notification: {}", node);
                                } catch (Exception ignored) {}
                            }
                            notifyListeners();
                        }
                    }
                } catch (Exception ex) {
                    if (isRunning) {
                        log.warn("PostgreSQL notification listener disconnected, retrying in 3s: {}", ex.getMessage());
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
        }, "Lro-Postgres-CDC-Listener");
        notificationListenerThread.setDaemon(true);
        notificationListenerThread.start();
    }

    public static String mapBankToSchema(String bankId) {
        if (bankId == null) return "bank_large";
        return switch (bankId) {
            case "BANK-01-LRG" -> "bank_large";
            case "BANK-02-MED" -> "bank_medium";
            case "BANK-03-SML" -> "bank_small";
            case "BANK-04-DST" -> "bank_distressed";
            default -> "bank_large";
        };
    }

    @Override
    public List<BankProfile> getAllBankProfiles() {
        List<BankProfile> list = new ArrayList<>();
        String[] bankIds = {"BANK-01-LRG", "BANK-02-MED", "BANK-03-SML", "BANK-04-DST"};
        for (String id : bankIds) {
            getBankProfile(id).ifPresent(list::add);
        }
        return list;
    }

    @Override
    public Optional<BankProfile> getBankProfile(String bankId) {
        String schema = mapBankToSchema(bankId);
        String sql = "SELECT " +
                "(SELECT coalesce(sum(available_balance), 0) FROM " + schema + ".accounts) as total_deposits, " +
                "(SELECT coalesce(sum(principal_outstanding), 0) FROM " + schema + ".loans) as total_advances, " +
                "(SELECT coalesce(sum(principal_outstanding), 0) FROM " + schema + ".loans WHERE days_past_due > 90) as npa_advances, " +
                "(SELECT coalesce(sum(market_value), 0) FROM " + schema + ".treasury_holdings) as securities, " +
                "(SELECT count(*) FROM " + schema + ".customers) as cust_count, " +
                "(SELECT count(*) FROM " + schema + ".accounts) as acc_count;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                BigDecimal deposits = rs.getBigDecimal("total_deposits");
                BigDecimal advances = rs.getBigDecimal("total_advances");
                BigDecimal npaAdvances = rs.getBigDecimal("npa_advances");
                BigDecimal securities = rs.getBigDecimal("securities");

                BigDecimal grossNpaRatio = advances.compareTo(BigDecimal.ZERO) > 0
                        ? npaAdvances.multiply(BigDecimal.valueOf(100)).divide(advances, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                BigDecimal nim = "BANK-04-DST".equals(bankId) ? new BigDecimal("2.10") :
                        "BANK-03-SML".equals(bankId) ? new BigDecimal("3.45") :
                        "BANK-02-MED".equals(bankId) ? new BigDecimal("3.80") : new BigDecimal("4.15");

                BigDecimal cashReserve = deposits.multiply(BigDecimal.valueOf(0.045)).setScale(2, RoundingMode.HALF_UP);

                String name = "BANK-01-LRG".equals(bankId) ? "Apex National Commercial Bank" :
                        "BANK-02-MED".equals(bankId) ? "Pragati Sahakari Commercial Bank" :
                        "BANK-03-SML".equals(bankId) ? "Kalyan Small Finance Bank" : "Deccan Distressed Mercantile Bank";

                long custCount = rs.getLong("cust_count");
                BigDecimal totalLiabilities = deposits.multiply(BigDecimal.valueOf(1.08)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalAssets = totalLiabilities.multiply(BigDecimal.valueOf(1.14)).setScale(2, RoundingMode.HALF_UP);
                String tierDesc = "BANK-01-LRG".equals(bankId) ? "Tier-1 Large Commercial Bank" :
                        "BANK-02-MED".equals(bankId) ? "Tier-2 Mid-Sized Scheduled Bank" :
                        "BANK-03-SML".equals(bankId) ? "Tier-3 Small Finance Bank" : "Tier-4 Stressed Urban Cooperative Bank";

                String health = "BANK-04-DST".equals(bankId) ? "Distressed" :
                        "BANK-03-SML".equals(bankId) ? "Moderate" : "Healthy";

                return Optional.of(new BankProfile(
                        bankId,
                        name,
                        tierDesc,
                        custCount,
                        totalAssets,
                        totalLiabilities,
                        deposits,
                        advances,
                        cashReserve,
                        securities,
                        grossNpaRatio,
                        nim,
                        health,
                        Instant.now()
                ));
            }
        } catch (SQLException ex) {
            log.error("Failed to query bank profile for {}: {}", bankId, ex.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<CustomerRecord> getCustomers(String bankId, String query, String segmentFilter) {
        String schema = mapBankToSchema(bankId);
        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.customer_id,
                c.full_name,
                c.segment,
                c.kyc_status,
                c.location_category,
                c.risk_category,
                c.branch_name,
                (COALESCE(a.acc_count, 0) + COALESCE(l.loan_count, 0)) AS linked_accounts,
                (COALESCE(a.acc_balance, 0) + COALESCE(l.loan_balance, 0)) AS relationship_value
            FROM %s.customers c
            LEFT JOIN (
                SELECT customer_id, COUNT(*) AS acc_count, SUM(available_balance) AS acc_balance
                FROM %s.accounts
                GROUP BY customer_id
            ) a ON a.customer_id = c.customer_id
            LEFT JOIN (
                SELECT customer_id, COUNT(*) AS loan_count, SUM(principal_outstanding) AS loan_balance
                FROM %s.loans
                GROUP BY customer_id
            ) l ON l.customer_id = c.customer_id
            WHERE 1=1\s""".formatted(schema, schema, schema));

        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (lower(c.full_name) LIKE ? OR lower(c.customer_id) LIKE ?) ");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }
        if (segmentFilter != null && !segmentFilter.equalsIgnoreCase("All")) {
            sql.append(" AND lower(c.segment) = lower(?) ");
            params.add(segmentFilter.trim());
        }
        sql.append(" ORDER BY c.customer_id ASC LIMIT 5000;");

        List<CustomerRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int linkedAccounts = rs.getInt("linked_accounts");
                    BigDecimal relVal = rs.getBigDecimal("relationship_value");
                    list.add(new CustomerRecord(
                            rs.getString("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("segment"),
                            rs.getString("kyc_status"),
                            rs.getString("location_category"),
                            rs.getString("risk_category"),
                            linkedAccounts,
                            relVal != null ? relVal : BigDecimal.ZERO,
                            rs.getString("branch_name")
                    ));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to query customers for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public List<AccountRecord> getAccounts(String bankId, String query) {
        return getAccounts(bankId, query, "All");
    }

    @Override
    public List<AccountRecord> getAccounts(String bankId, String query, String accountTypeFilter) {
        String schema = mapBankToSchema(bankId);
        StringBuilder sql = new StringBuilder("SELECT account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at FROM " + schema + ".accounts WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (lower(account_id) LIKE ? OR lower(customer_id) LIKE ?) ");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }
        if (accountTypeFilter != null && !accountTypeFilter.equalsIgnoreCase("All")) {
            sql.append(" AND lower(account_type) = lower(?) ");
            params.add(accountTypeFilter.trim());
        }
        sql.append(" ORDER BY account_id ASC LIMIT 10000;");

        List<AccountRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new AccountRecord(
                            rs.getString("account_id"),
                            rs.getString("customer_id"),
                            rs.getString("account_type"),
                            rs.getString("currency"),
                            rs.getBigDecimal("available_balance"),
                            rs.getBigDecimal("hold_amount"),
                            rs.getString("status"),
                            ts != null ? ts.toInstant() : Instant.now()
                    ));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to query accounts for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public List<DepositRecord> getDeposits(String bankId) {
        return Collections.emptyList();
    }

    @Override
    public List<DepositRecord> getDeposits(String bankId, String query) {
        return Collections.emptyList();
    }

    @Override
    public List<LoanRecord> getLoans(String bankId, String npaFilter) {
        return getLoans(bankId, "", npaFilter);
    }

    @Override
    public List<LoanRecord> getLoans(String bankId, String query, String npaFilter) {
        String schema = mapBankToSchema(bankId);
        StringBuilder sql = new StringBuilder("SELECT loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding, interest_rate_pct, days_past_due, irac_classification, collateral_value, next_emi_due_date, monthly_emi FROM " + schema + ".loans WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (lower(loan_id) LIKE ? OR lower(customer_id) LIKE ? OR lower(loan_type) LIKE ?) ");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (npaFilter != null && !npaFilter.equalsIgnoreCase("All")) {
            if (npaFilter.equalsIgnoreCase("NPA Only")) {
                sql.append(" AND days_past_due > 90 ");
            } else {
                sql.append(" AND lower(irac_classification) = lower(?) ");
                params.add(npaFilter.trim());
            }
        }
        sql.append(" ORDER BY loan_id ASC LIMIT 10000;");

        List<LoanRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("next_emi_due_date");
                    list.add(new LoanRecord(
                            rs.getString("loan_id"),
                            rs.getString("customer_id"),
                            rs.getString("loan_type"),
                            rs.getBigDecimal("sanctioned_amount"),
                            rs.getBigDecimal("principal_outstanding"),
                            rs.getBigDecimal("interest_rate_pct"),
                            rs.getInt("days_past_due"),
                            rs.getString("irac_classification"),
                            rs.getBigDecimal("collateral_value"),
                            d != null ? d.toLocalDate() : LocalDate.now().plusDays(15),
                            rs.getBigDecimal("monthly_emi")
                    ));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to query loans for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public List<TreasuryHolding> getTreasuryHoldings(String bankId) {
        return getTreasuryHoldings(bankId, "", "All");
    }

    @Override
    public List<TreasuryHolding> getTreasuryHoldings(String bankId, String query, String categoryFilter) {
        String schema = mapBankToSchema(bankId);
        StringBuilder sql = new StringBuilder("SELECT holding_id, instrument_name, category, quantity, face_value_unit, book_value, market_value, encumbrance_status, maturity_date, coupon_or_yield_pct, last_valuation_time FROM " + schema + ".treasury_holdings WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (lower(holding_id) LIKE ? OR lower(instrument_name) LIKE ? OR lower(category) LIKE ?) ");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("All")) {
            sql.append(" AND lower(category) = lower(?) ");
            params.add(categoryFilter.trim());
        }
        sql.append(" ORDER BY holding_id ASC LIMIT 10000;");

        List<TreasuryHolding> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date mDate = rs.getDate("maturity_date");
                    Timestamp vTime = rs.getTimestamp("last_valuation_time");
                    BigDecimal mktVal = rs.getBigDecimal("market_value");
                    BigDecimal proceeds = mktVal.multiply(new BigDecimal("0.96")).setScale(2, RoundingMode.HALF_UP);

                    list.add(new TreasuryHolding(
                            rs.getString("holding_id"),
                            rs.getString("instrument_name"),
                            rs.getString("category"),
                            rs.getLong("quantity"),
                            rs.getBigDecimal("face_value_unit"),
                            rs.getBigDecimal("book_value"),
                            mktVal,
                            proceeds,
                            rs.getString("encumbrance_status"),
                            mDate != null ? mDate.toLocalDate() : null,
                            rs.getBigDecimal("coupon_or_yield_pct"),
                            vTime != null ? vTime.toInstant() : Instant.now()
                    ));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to query treasury holdings for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public List<LedgerEntry> getLedgerEntries(String bankId) {
        String schema = mapBankToSchema(bankId);
        String sql = "SELECT entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status FROM " + schema + ".ledger_entries ORDER BY timestamp DESC LIMIT 2000;";

        List<LedgerEntry> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                list.add(new LedgerEntry(
                        rs.getString("entry_id"),
                        rs.getString("transaction_ref"),
                        ts != null ? ts.toInstant() : Instant.now(),
                        rs.getString("account_reference"),
                        rs.getString("counterparty_ref"),
                        rs.getBigDecimal("debit_inr"),
                        rs.getBigDecimal("credit_inr"),
                        rs.getString("type"),
                        rs.getString("method"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            log.error("Failed to query ledger entries for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public List<CashFlowBucket> getCashFlowLadder(String bankId) {
        boolean isDistressed = "BANK-04-DST".equals(bankId);
        List<CashFlowBucket> list = new ArrayList<>();

        BigDecimal b1In = new BigDecimal("450000000.00");
        BigDecimal b1Out = isDistressed ? new BigDecimal("850000000.00") : new BigDecimal("420000000.00");
        BigDecimal gap1 = b1In.subtract(b1Out);

        BigDecimal b2In = new BigDecimal("1200000000.00");
        BigDecimal b2Out = isDistressed ? new BigDecimal("1900000000.00") : new BigDecimal("1100000000.00");
        BigDecimal gap2 = b2In.subtract(b2Out);

        BigDecimal b3In = new BigDecimal("3500000000.00");
        BigDecimal b3Out = isDistressed ? new BigDecimal("4200000000.00") : new BigDecimal("3200000000.00");
        BigDecimal gap3 = b3In.subtract(b3Out);

        BigDecimal b4In = new BigDecimal("8200000000.00");
        BigDecimal b4Out = isDistressed ? new BigDecimal("8900000000.00") : new BigDecimal("7400000000.00");
        BigDecimal gap4 = b4In.subtract(b4Out);

        BigDecimal b5In = new BigDecimal("25000000000.00");
        BigDecimal b5Out = new BigDecimal("21000000000.00");
        BigDecimal gap5 = b5In.subtract(b5Out);

        BigDecimal cum1 = gap1;
        BigDecimal cum2 = cum1.add(gap2);
        BigDecimal cum3 = cum2.add(gap3);
        BigDecimal cum4 = cum3.add(gap4);
        BigDecimal cum5 = cum4.add(gap5);

        list.add(new CashFlowBucket("Same Day (1D)", b1In, b1Out, gap1, cum1));
        list.add(new CashFlowBucket("2 - 7 Days", b2In, b2Out, gap2, cum2));
        list.add(new CashFlowBucket("8 - 30 Days", b3In, b3Out, gap3, cum3));
        list.add(new CashFlowBucket("31 - 90 Days", b4In, b4Out, gap4, cum4));
        list.add(new CashFlowBucket("> 90 Days", b5In, b5Out, gap5, cum5));

        return list;
    }

    @Override
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId) {
        return getBalanceSheetRecords(bankId, "", "All", 2000);
    }

    @Override
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId, String query, String categoryFilter) {
        return getBalanceSheetRecords(bankId, query, categoryFilter, 2000);
    }

    @Override
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId, String query, String categoryFilter, int limit) {
        String schema = mapBankToSchema(bankId);
        StringBuilder sql = new StringBuilder("SELECT record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status FROM " + schema + ".balance_sheet WHERE 1=1 ");

        boolean isAllCategories = categoryFilter == null || categoryFilter.isBlank() || categoryFilter.trim().toLowerCase().startsWith("all");
        if (!isAllCategories) {
            sql.append(" AND category = '").append(categoryFilter.trim()).append("'");
        }
        if (query != null && !query.isBlank()) {
            String q = query.trim().replace("'", "''");
            sql.append(" AND (record_id ILIKE '%").append(q).append("%' OR account_reference ILIKE '%").append(q)
                    .append("%' OR line_item ILIKE '%").append(q).append("%' OR type ILIKE '%").append(q)
                    .append("%' OR method ILIKE '%").append(q).append("%')");
        }
        sql.append(" ORDER BY timestamp DESC LIMIT ").append(limit > 0 ? limit : 2000).append(";");

        List<BalanceSheetRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                list.add(new BalanceSheetRecord(
                        rs.getString("record_id"),
                        ts != null ? ts.toInstant() : Instant.now(),
                        rs.getString("account_reference"),
                        rs.getString("line_item"),
                        rs.getString("category"),
                        rs.getBigDecimal("debit_inr"),
                        rs.getBigDecimal("credit_inr"),
                        rs.getBigDecimal("running_balance_inr"),
                        rs.getString("type"),
                        rs.getString("method"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            log.error("Failed to query balance sheet records for {}: {}", bankId, ex.getMessage());
        }
        return list;
    }

    @Override
    public BalanceSheetSummary getBalanceSheetSummary(String bankId) {
        String schema = mapBankToSchema(bankId);
        String sql = "SELECT " +
                "(SELECT coalesce(sum(available_balance), 0) FROM " + schema + ".accounts) as total_deposits, " +
                "(SELECT coalesce(sum(principal_outstanding), 0) FROM " + schema + ".loans) as total_advances, " +
                "(SELECT coalesce(sum(market_value), 0) FROM " + schema + ".treasury_holdings) as securities, " +
                "(SELECT count(*) FROM " + schema + ".balance_sheet) as total_bs_txns;";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                BigDecimal casaDeposits = rs.getBigDecimal("total_deposits");
                BigDecimal loansAndAdvances = rs.getBigDecimal("total_advances");
                BigDecimal treasuryInvestments = rs.getBigDecimal("securities");
                long totalTxns = rs.getLong("total_bs_txns");

                // Realistic banking balance sheet ratios & components
                BigDecimal cashAndRbiReserves = casaDeposits.multiply(new BigDecimal("0.045")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal termDeposits = casaDeposits.multiply(new BigDecimal("0.65")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal interbankBorrowings = casaDeposits.add(termDeposits).multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal otherLiabilities = casaDeposits.add(termDeposits).multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

                BigDecimal totalLiabilities = casaDeposits.add(termDeposits).add(interbankBorrowings).add(otherLiabilities);

                // Other assets (premises, fixed assets, clearing balances)
                BigDecimal otherAssets = totalLiabilities.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalAssets = loansAndAdvances.add(treasuryInvestments).add(cashAndRbiReserves).add(otherAssets);

                // Capital & Reserves to enforce exact balance: Total Assets = Total Liabilities + Total Equity
                BigDecimal totalEquity = totalAssets.subtract(totalLiabilities);
                BigDecimal shareCapital = totalAssets.multiply(new BigDecimal("0.06")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal reservesAndSurplus = totalEquity.subtract(shareCapital);

                BigDecimal netWorth = totalEquity;
                boolean isBalanced = totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;
                BigDecimal discrepancy = totalAssets.subtract(totalLiabilities.add(totalEquity)).abs();

                return new BalanceSheetSummary(
                        totalAssets,
                        totalLiabilities,
                        totalEquity,
                        netWorth,
                        isBalanced,
                        discrepancy,
                        loansAndAdvances,
                        treasuryInvestments,
                        cashAndRbiReserves,
                        otherAssets,
                        casaDeposits,
                        termDeposits,
                        interbankBorrowings,
                        otherLiabilities,
                        shareCapital,
                        reservesAndSurplus,
                        totalTxns
                );
            }
        } catch (SQLException ex) {
            log.error("Failed to query balance sheet summary for {}: {}", bankId, ex.getMessage());
        }

        return new BalanceSheetSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
    }

    // =========================================================================
    // Real-Time Atomic Database Transactions (App -> PostgreSQL Live Updates)
    // =========================================================================

    @Override
    public boolean openSyntheticAccount(String bankId, String customerId, String accountType, BigDecimal initialDeposit) {
        String schema = mapBankToSchema(bankId);
        String sql = "INSERT INTO " + schema + ".accounts (account_id, customer_id, account_type, currency, available_balance, hold_amount, status, created_at) " +
                "VALUES ('ACC-' || to_char(nextval('" + schema + "_acc_seq'), 'FM00000'), ?, ?, 'INR', ?, 0.00, 'Active', CURRENT_TIMESTAMP);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, accountType);
            ps.setBigDecimal(3, initialDeposit);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            log.error("Failed to open account in {}: {}", schema, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean recordDepositTransaction(String bankId, String accountId, BigDecimal amount, String type) {
        String schema = mapBankToSchema(bankId);
        String updateSql = "UPDATE " + schema + ".accounts SET available_balance = available_balance + ? WHERE account_id = ?;";
        String ledgerSql = "INSERT INTO " + schema + ".ledger_entries (entry_id, transaction_ref, timestamp, account_reference, counterparty_ref, debit_inr, credit_inr, type, method, status) " +
                "VALUES ('LED-' || to_char(random() * 1000000, 'FM000000'), 'TXN-' || to_char(random() * 1000000, 'FM000000'), CURRENT_TIMESTAMP, ?, 'CASH_CLEARING', ?, 0.00, ?, ?, 'Posted');";
        String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                "VALUES ('BS-' || to_char(random() * 10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'CASA Deposits', 'Liability', 0.00, ?, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) + ?, 'Deposits', 'Cash', 'Posted');";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psLedger = conn.prepareStatement(ledgerSql);
                 PreparedStatement psBs = conn.prepareStatement(bsSql)) {

                psUpdate.setBigDecimal(1, amount);
                psUpdate.setString(2, accountId);
                psUpdate.executeUpdate();

                psLedger.setString(1, accountId);
                psLedger.setBigDecimal(2, amount);
                psLedger.setString(3, "Deposits");
                psLedger.setString(4, "Cash");
                psLedger.executeUpdate();

                psBs.setString(1, accountId);
                psBs.setBigDecimal(2, amount);
                psBs.setBigDecimal(3, amount);
                psBs.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            log.error("Deposit transaction failed in {}: {}", schema, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean originateDemoLoan(String bankId, String customerId, String loanType, BigDecimal amount, BigDecimal rate) {
        String schema = mapBankToSchema(bankId);
        String lnId = "LN-DEMO-" + (System.currentTimeMillis() % 100000);
        String insertSql = "INSERT INTO " + schema + ".loans (loan_id, customer_id, loan_type, sanctioned_amount, principal_outstanding, interest_rate_pct, days_past_due, irac_classification, collateral_value, next_emi_due_date, monthly_emi) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0, 'Standard', ?, CURRENT_DATE + interval '30 day', ?);";

        BigDecimal monthlyEmi = amount.multiply(new BigDecimal("0.015")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal collateral = amount.multiply(new BigDecimal("1.25")).setScale(2, RoundingMode.HALF_UP);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, lnId);
                ps.setString(2, customerId);
                ps.setString(3, loanType);
                ps.setBigDecimal(4, amount);
                ps.setBigDecimal(5, amount);
                ps.setBigDecimal(6, rate);
                ps.setBigDecimal(7, collateral);
                ps.setBigDecimal(8, monthlyEmi);
                ps.executeUpdate();

                // Credit customer account with loan proceeds
                String creditSql = "UPDATE " + schema + ".accounts SET available_balance = available_balance + ? WHERE customer_id = ? AND account_type IN ('Saving', 'Current') LIMIT 1;";
                try (PreparedStatement psCredit = conn.prepareStatement(creditSql)) {
                    psCredit.setBigDecimal(1, amount);
                    psCredit.setString(2, customerId);
                    psCredit.executeUpdate();
                }

                String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                        "VALUES ('BS-' || to_char(random() * 10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'Loans & Advances', 'Asset', ?, 0.00, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) + ?, 'Loan Disbursement', 'RTGS', 'Posted');";
                try (PreparedStatement psBs = conn.prepareStatement(bsSql)) {
                    psBs.setString(1, customerId);
                    psBs.setBigDecimal(2, amount);
                    psBs.setBigDecimal(3, amount);
                    psBs.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            log.error("Failed to originate demo loan in {}: {}", schema, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean recordLoanRepayment(String bankId, String loanId, BigDecimal emiAmount) {
        String schema = mapBankToSchema(bankId);
        String updateLoan = "UPDATE " + schema + ".loans SET principal_outstanding = GREATEST(0.00, principal_outstanding - ?), days_past_due = 0 WHERE loan_id = ? RETURNING customer_id;";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateLoan)) {
                ps.setBigDecimal(1, emiAmount);
                ps.setString(2, loanId);
                String custId = null;
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        custId = rs.getString("customer_id");
                    }
                }

                if (custId != null) {
                    String debitAcc = "UPDATE " + schema + ".accounts SET available_balance = available_balance - ? WHERE customer_id = ? AND available_balance >= ? LIMIT 1;";
                    try (PreparedStatement psDebit = conn.prepareStatement(debitAcc)) {
                        psDebit.setBigDecimal(1, emiAmount);
                        psDebit.setString(2, custId);
                        psDebit.setBigDecimal(3, emiAmount);
                        psDebit.executeUpdate();
                    }

                    String bsSql = "INSERT INTO " + schema + ".balance_sheet (record_id, timestamp, account_reference, line_item, category, debit_inr, credit_inr, running_balance_inr, type, method, status) " +
                            "VALUES ('BS-' || to_char(random() * 10000000, 'FM0000000'), CURRENT_TIMESTAMP, ?, 'Loans & Advances', 'Asset', 0.00, ?, (SELECT coalesce(running_balance_inr, 200000000000) FROM " + schema + ".balance_sheet ORDER BY timestamp DESC LIMIT 1) - ?, 'EMI Repayment', 'UPI', 'Posted');";
                    try (PreparedStatement psBs = conn.prepareStatement(bsSql)) {
                        psBs.setString(1, custId);
                        psBs.setBigDecimal(2, emiAmount);
                        psBs.setBigDecimal(3, emiAmount);
                        psBs.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            log.error("Loan repayment failed in {}: {}", schema, ex.getMessage());
            return false;
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean isConnected() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        isRunning = false;
        if (notificationListenerThread != null) {
            notificationListenerThread.interrupt();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
