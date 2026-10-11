package com.lro.frontend.bank.service;

import com.lro.frontend.bank.model.BankModels.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Deterministic fixture-backed mock service implementation for Bank Simulator.
 * Provides isolated data structures for the 4 fictional Indian banks.
 */
public class MockBankService implements BankService {

    private final List<BankProfile> profiles = new ArrayList<>();
    private final Map<String, List<CustomerRecord>> customerData = new ConcurrentHashMap<>();
    private final Map<String, List<AccountRecord>> accountData = new ConcurrentHashMap<>();
    private final Map<String, List<DepositRecord>> depositData = new ConcurrentHashMap<>();
    private final Map<String, List<LoanRecord>> loanData = new ConcurrentHashMap<>();
    private final Map<String, List<TreasuryHolding>> treasuryData = new ConcurrentHashMap<>();
    private final Map<String, List<LedgerEntry>> ledgerData = new ConcurrentHashMap<>();
    private final Map<String, List<CashFlowBucket>> ladderData = new ConcurrentHashMap<>();

    public MockBankService() {
        seedAllData();
    }

    private void seedAllData() {
        Instant now = Instant.now();

        // 1. Bank Profiles
        profiles.add(new BankProfile(
                "BANK-01-LRG",
                "Apex National Bank (Synthetic)",
                "Tier-1 Large Commercial Bank",
                10500L,
                new BigDecimal("520000000000.00"), // ₹ 52,000 Cr Assets
                new BigDecimal("468000000000.00"), // ₹ 46,800 Cr Liab
                new BigDecimal("390000000000.00"), // ₹ 39,000 Cr Deposits
                new BigDecimal("312000000000.00"), // ₹ 31,200 Cr Loans
                new BigDecimal("42000000000.00"),  // ₹ 4,200 Cr Cash
                new BigDecimal("145000000000.00"), // ₹ 14,500 Cr Securities
                new BigDecimal("1.25"),            // 1.25% Gross NPA
                new BigDecimal("3.85"),            // 3.85% NIM
                "Healthy",
                now
        ));

        profiles.add(new BankProfile(
                "BANK-02-MED",
                "Bharat Mercantile Bank (Synthetic)",
                "Tier-2 Mid-Sized Scheduled Bank",
                7850L,
                new BigDecimal("215000000000.00"), // ₹ 21,500 Cr
                new BigDecimal("193000000000.00"),
                new BigDecimal("162000000000.00"),
                new BigDecimal("128000000000.00"),
                new BigDecimal("16500000000.00"),
                new BigDecimal("58000000000.00"),
                new BigDecimal("2.40"),
                new BigDecimal("3.45"),
                "Healthy",
                now
        ));

        profiles.add(new BankProfile(
                "BANK-03-SML",
                "Sahyadri Urban Co-operative Bank (Synthetic)",
                "Tier-3 Small Regional Bank",
                5200L,
                new BigDecimal("84000000000.00"), // ₹ 8,400 Cr
                new BigDecimal("75500000000.00"),
                new BigDecimal("64000000000.00"),
                new BigDecimal("49500000000.00"),
                new BigDecimal("7200000000.00"),
                new BigDecimal("22500000000.00"),
                new BigDecimal("1.80"),
                new BigDecimal("3.60"),
                "Healthy",
                now
        ));

        profiles.add(new BankProfile(
                "BANK-04-DST",
                "Kalyan Industrial Credit Bank (Synthetic)",
                "Tier-3 Distressed Turnaround Bank",
                5100L,
                new BigDecimal("91000000000.00"), // ₹ 9,100 Cr
                new BigDecimal("86500000000.00"),
                new BigDecimal("59000000000.00"), // Strained deposits
                new BigDecimal("58000000000.00"),
                new BigDecimal("3100000000.00"),  // Low cash reserve
                new BigDecimal("21000000000.00"),
                new BigDecimal("14.80"),          // 14.80% High NPA
                new BigDecimal("1.95"),           // Compressed margin
                "Distressed",
                now
        ));

        // Seed details for each bank
        for (BankProfile p : profiles) {
            seedCustomers(p.bankId(), p.healthStatus());
            seedAccounts(p.bankId());
            seedDeposits(p.bankId());
            seedLoans(p.bankId(), p.healthStatus());
            seedTreasury(p.bankId());
            seedLedger(p.bankId());
            seedLadder(p.bankId(), p.healthStatus());
        }
    }

    private void seedCustomers(String bankId, String health) {
        List<CustomerRecord> list = new ArrayList<>();
        String[] retailNames = {"Aarav Sharma", "Priya Nair", "Rohan Mehta", "Ananya Deshmukh", "Vikram Sen", "Sunita Patil"};
        String[] smeNames = {"Omkar Logistics Pvt Ltd", "Ganesh Auto Spares", "Shree Textiles LLP", "Sai Electronics & Tech"};
        String[] corpNames = {"Surya Infrastructure Corp", "Titanium Solar Ventures Ltd", "Kaveri Agro Exports Ltd"};

        int id = 1001;
        for (int i = 0; i < retailNames.length; i++) {
            String kyc = (i % 3 == 0 && health.equals("Distressed")) ? "Not Verified" : "Verified";
            String loc = switch (i % 4) {
                case 0 -> "Tier 1";
                case 1 -> "Tier 2";
                case 2 -> "Tier 3";
                default -> "Rural";
            };
            list.add(new CustomerRecord(bankId + "-CUST-" + id++, retailNames[i], "Retail", kyc, loc, "Low", 2, new BigDecimal("1850000.00"), "Fort Nariman Point Branch"));
        }
        for (int i = 0; i < smeNames.length; i++) {
            String kyc = (i == 1 || (i == 2 && health.equals("Distressed"))) ? "Not Verified" : "Verified";
            String loc = switch (i % 3) {
                case 0 -> "Tier 2";
                case 1 -> "Tier 3";
                default -> "Rural";
            };
            list.add(new CustomerRecord(bankId + "-CUST-" + id++, smeNames[i], "SME", kyc, loc, health.equals("Distressed") ? "High" : "Medium", 3, new BigDecimal("12400000.00"), "MIDC Andheri Branch"));
        }
        for (int i = 0; i < corpNames.length; i++) {
            String kyc = (i == 2 && health.equals("Distressed")) ? "Not Verified" : "Verified";
            String loc = switch (i % 3) {
                case 0 -> "Tier 1";
                case 1 -> "Tier 2";
                default -> "Rural";
            };
            list.add(new CustomerRecord(bankId + "-CUST-" + id++, corpNames[i], "Corporate", kyc, loc, health.equals("Distressed") ? "High" : "Low", 4, new BigDecimal("85000000.00"), "Corporate Finance Branch"));
        }
        customerData.put(bankId, list);
    }

    private void seedAccounts(String bankId) {
        List<AccountRecord> list = new ArrayList<>();
        List<CustomerRecord> custs = customerData.get(bankId);
        int accId = 5001;
        Instant now = Instant.now();
        String[] accountTypes = {"Saving", "Salary", "Current", "Fixed Deposit", "Recurring Deposit", "NRI", "Demat"};
        for (int i = 0; i < custs.size(); i++) {
            CustomerRecord c = custs.get(i);
            String primaryType = accountTypes[i % accountTypes.length];
            BigDecimal bal = switch (primaryType) {
                case "Current" -> new BigDecimal("3500000.00");
                case "Fixed Deposit" -> new BigDecimal("1500000.00");
                case "Recurring Deposit" -> new BigDecimal("250000.00");
                case "NRI" -> new BigDecimal("5000000.00");
                case "Salary" -> new BigDecimal("350000.00");
                case "Demat" -> new BigDecimal("120000.00");
                default -> new BigDecimal("450000.00");
            };
            list.add(new AccountRecord(bankId + "-ACC-" + accId++, c.customerId(), primaryType, "INR", bal, BigDecimal.ZERO, "Active", now));
            if (!c.segment().equals("Retail")) {
                String secondaryType = accountTypes[(i + 2) % accountTypes.length];
                list.add(new AccountRecord(bankId + "-ACC-" + accId++, c.customerId(), secondaryType, "INR", new BigDecimal("3200000.00"), new BigDecimal("100000.00"), "Active", now));
            }
        }
        accountData.put(bankId, list);
    }

    private void seedDeposits(String bankId) {
        List<DepositRecord> list = new ArrayList<>();
        List<CustomerRecord> custs = customerData.get(bankId);
        LocalDate today = LocalDate.now();
        int dId = 701;
        for (int i = 0; i < Math.min(custs.size(), 6); i++) {
            CustomerRecord c = custs.get(i);
            list.add(new DepositRecord(
                    bankId + "-DEP-" + dId++,
                    c.customerId(),
                    "Fixed Deposit",
                    new BigDecimal("1000000.00"),
                    new BigDecimal("7.25"),
                    today.minusMonths(6),
                    today.plusMonths(6),
                    new BigDecimal("1074000.00"),
                    "Auto-Renew Principal & Interest"
            ));
        }
        depositData.put(bankId, list);
    }

    private void seedLoans(String bankId, String health) {
        List<LoanRecord> list = new ArrayList<>();
        List<CustomerRecord> custs = customerData.get(bankId);
        LocalDate today = LocalDate.now();
        int lId = 901;

        for (int i = 0; i < custs.size(); i++) {
            CustomerRecord c = custs.get(i);
            boolean isDistressed = health.equals("Distressed") && (i % 2 == 0);
            int dpd = isDistressed ? (i % 4 == 0 ? 95 : 45) : (i == 1 ? 15 : 0);
            String irac = dpd > 90 ? "Sub-Standard NPA" : (dpd > 60 ? "SMA-2" : (dpd > 30 ? "SMA-1" : (dpd > 0 ? "SMA-0" : "Standard")));

            list.add(new LoanRecord(
                    bankId + "-LN-" + lId++,
                    c.customerId(),
                    c.segment().equals("Corporate") ? "Working Capital Loan" : (c.segment().equals("SME") ? "MSME Term Loan" : "Home Loan"),
                    new BigDecimal("5000000.00"),
                    new BigDecimal("4200000.00"),
                    new BigDecimal("9.50"),
                    dpd,
                    irac,
                    new BigDecimal("6500000.00"),
                    today.plusDays(15),
                    new BigDecimal("55000.00")
            ));
        }
        loanData.put(bankId, list);
    }

    private void seedTreasury(String bankId) {
        List<TreasuryHolding> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        list.add(new TreasuryHolding(bankId + "-TRS-01", "7.18% GS 2033 (Govt of India)", "G-Sec", 500000L, new BigDecimal("100.00"), new BigDecimal("50000000.00"), new BigDecimal("50420000.00"), new BigDecimal("49800000.00"), "Unencumbered (Free)", today.plusYears(9), new BigDecimal("7.18"), now));
        list.add(new TreasuryHolding(bankId + "-TRS-02", "7.06% GS 2028 (Govt of India)", "G-Sec", 300000L, new BigDecimal("100.00"), new BigDecimal("30000000.00"), new BigDecimal("30150000.00"), new BigDecimal("29850000.00"), "Unencumbered (Free)", today.plusYears(4), new BigDecimal("7.06"), now));
        list.add(new TreasuryHolding(bankId + "-TRS-03", "91-Day Treasury Bill", "T-Bill", 400000L, new BigDecimal("100.00"), new BigDecimal("39300000.00"), new BigDecimal("39580000.00"), new BigDecimal("39450000.00"), "Unencumbered (Free)", today.plusDays(45), new BigDecimal("6.85"), now));
        list.add(new TreasuryHolding(bankId + "-TRS-04", "182-Day Treasury Bill", "T-Bill", 250000L, new BigDecimal("100.00"), new BigDecimal("24100000.00"), new BigDecimal("24350000.00"), new BigDecimal("24200000.00"), "Encumbered (Repo / Collateral)", today.plusDays(110), new BigDecimal("6.92"), now));
        list.add(new TreasuryHolding(bankId + "-TRS-05", "8.15% HDFC Bank AAA Bond 2029", "Corporate Bond", 100000L, new BigDecimal("1000.00"), new BigDecimal("100000000.00"), new BigDecimal("99800000.00"), new BigDecimal("96500000.00"), "Unencumbered (Free)", today.plusYears(5), new BigDecimal("8.15"), now));
        list.add(new TreasuryHolding(bankId + "-TRS-06", "Reliance Industries Eq (Simulated)", "Equity", 50000L, new BigDecimal("1250.00"), new BigDecimal("62500000.00"), new BigDecimal("68200000.00"), new BigDecimal("63500000.00"), "Unencumbered (Free)", null, new BigDecimal("0.00"), now));

        treasuryData.put(bankId, list);
    }

    private void seedLedger(String bankId) {
        List<LedgerEntry> list = new ArrayList<>();
        Instant now = Instant.now();

        list.add(new LedgerEntry("LGD-1001", "TXN-CCIL-881", now.minusSeconds(3600), "CASH_RBI_RESERVE", "INTERBANK_CLEARING", new BigDecimal("50000000.00"), BigDecimal.ZERO, "RTGS Interbank Settlement - CCIL Clearing", "Posted"));
        list.add(new LedgerEntry("LGD-1002", "TXN-DEP-401", now.minusSeconds(7200), "CASA_SAVINGS_LIAB", "CUSTOMER_WITHDRAWAL", BigDecimal.ZERO, new BigDecimal("1500000.00"), "High Value Commercial Cash Withdrawal", "Posted"));
        list.add(new LedgerEntry("LGD-1003", "TXN-SEC-112", now.minusSeconds(10800), "TREASURY_GSEC_HOLDINGS", "CASH_RBI_RESERVE", new BigDecimal("25000000.00"), BigDecimal.ZERO, "Secondary Market Purchase: 7.18% GS 2033", "Posted"));
        list.add(new LedgerEntry("LGD-1004", "TXN-EMI-992", now.minusSeconds(14400), "LOAN_PORTFOLIO_ASSET", "CASA_SAVINGS_LIAB", BigDecimal.ZERO, new BigDecimal("250000.00"), "Automated Loan EMI Batch Recovery", "Posted"));

        ledgerData.put(bankId, list);
    }

    private void seedLadder(String bankId, String health) {
        List<CashFlowBucket> list = new ArrayList<>();
        boolean isDistressed = health.equals("Distressed");

        BigDecimal b1In = new BigDecimal("450000000.00");
        BigDecimal b1Out = isDistressed ? new BigDecimal("850000000.00") : new BigDecimal("420000000.00");
        BigDecimal gap1 = b1In.subtract(b1Out);

        BigDecimal b2In = new BigDecimal("1200000000.00");
        BigDecimal b2Out = isDistressed ? new BigDecimal("1900000000.00") : new BigDecimal("1100000000.00");
        BigDecimal gap2 = b2In.subtract(b2Out);

        BigDecimal b3In = new BigDecimal("3500000000.00");
        BigDecimal b3Out = new BigDecimal("3200000000.00");
        BigDecimal gap3 = b3In.subtract(b3Out);

        BigDecimal b4In = new BigDecimal("8200000000.00");
        BigDecimal b4Out = new BigDecimal("7400000000.00");
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

        ladderData.put(bankId, list);
    }

    @Override
    public List<BankProfile> getAllBankProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    @Override
    public Optional<BankProfile> getBankProfile(String bankId) {
        return profiles.stream().filter(p -> p.bankId().equalsIgnoreCase(bankId)).findFirst();
    }

    @Override
    public List<CustomerRecord> getCustomers(String bankId, String query, String segmentFilter) {
        List<CustomerRecord> all = customerData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(c -> query == null || query.isBlank() || c.fullName().toLowerCase().contains(query.toLowerCase()) || c.customerId().toLowerCase().contains(query.toLowerCase()))
                .filter(c -> segmentFilter == null || segmentFilter.equalsIgnoreCase("All") || c.segment().equalsIgnoreCase(segmentFilter))
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountRecord> getAccounts(String bankId, String query) {
        List<AccountRecord> all = accountData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(a -> query == null || query.isBlank() || a.accountId().toLowerCase().contains(query.toLowerCase()) || a.customerId().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DepositRecord> getDeposits(String bankId) {
        return customerData.containsKey(bankId) ? depositData.getOrDefault(bankId, List.of()) : List.of();
    }

    @Override
    public List<LoanRecord> getLoans(String bankId, String npaFilter) {
        List<LoanRecord> all = loanData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(l -> npaFilter == null || npaFilter.equalsIgnoreCase("All") || (npaFilter.equalsIgnoreCase("NPA Only") ? l.daysPastDue() > 90 : l.iracClassification().equalsIgnoreCase(npaFilter)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TreasuryHolding> getTreasuryHoldings(String bankId) {
        return treasuryData.getOrDefault(bankId, List.of());
    }

    @Override
    public List<LedgerEntry> getLedgerEntries(String bankId) {
        return ledgerData.getOrDefault(bankId, List.of());
    }

    @Override
    public List<CashFlowBucket> getCashFlowLadder(String bankId) {
        return ladderData.getOrDefault(bankId, List.of());
    }

    @Override
    public boolean openSyntheticAccount(String bankId, String customerId, String accountType, BigDecimal initialDeposit) {
        if (bankId == null || customerId == null || initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        String newAccId = bankId + "-ACC-" + (System.currentTimeMillis() % 100000);
        AccountRecord rec = new AccountRecord(newAccId, customerId, accountType, "INR", initialDeposit, BigDecimal.ZERO, "Active", Instant.now());
        accountData.computeIfAbsent(bankId, k -> new ArrayList<>()).add(0, rec);
        return true;
    }

    @Override
    public boolean recordDepositTransaction(String bankId, String accountId, BigDecimal amount, String type) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        List<LedgerEntry> entries = ledgerData.computeIfAbsent(bankId, k -> new ArrayList<>());
        entries.add(0, new LedgerEntry(
                "LGD-" + (System.currentTimeMillis() % 100000),
                "TXN-SIM-" + (System.currentTimeMillis() % 10000),
                Instant.now(),
                accountId,
                type.equalsIgnoreCase("Deposit") ? "CASH_VAULT" : "EXTERNAL_BENEFICIARY",
                type.equalsIgnoreCase("Deposit") ? amount : BigDecimal.ZERO,
                type.equalsIgnoreCase("Withdrawal") ? amount : BigDecimal.ZERO,
                "Demo " + type + " Posting",
                "Posted"
        ));
        return true;
    }

    @Override
    public boolean originateDemoLoan(String bankId, String customerId, String loanType, BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        String newLoanId = bankId + "-LN-" + (System.currentTimeMillis() % 100000);
        LoanRecord rec = new LoanRecord(
                newLoanId, customerId, loanType, amount, amount, rate, 0, "Standard",
                amount.multiply(new BigDecimal("1.25")), LocalDate.now().plusMonths(1), amount.divide(new BigDecimal("120"), 2, java.math.RoundingMode.HALF_UP)
        );
        loanData.computeIfAbsent(bankId, k -> new ArrayList<>()).add(0, rec);
        return true;
    }

    @Override
    public boolean recordLoanRepayment(String bankId, String loanId, BigDecimal emiAmount) {
        if (emiAmount == null || emiAmount.compareTo(BigDecimal.ZERO) <= 0) return false;
        List<LedgerEntry> entries = ledgerData.computeIfAbsent(bankId, k -> new ArrayList<>());
        entries.add(0, new LedgerEntry(
                "LGD-" + (System.currentTimeMillis() % 100000),
                "EMI-REC-" + loanId,
                Instant.now(),
                "LOAN_PRINCIPAL_RECOVERY",
                "CASA_COLLECTION_POOL",
                BigDecimal.ZERO,
                emiAmount,
                "Demo EMI Repayment credited against " + loanId,
                "Posted"
        ));
        return true;
    }
}
