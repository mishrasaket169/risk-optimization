package com.lro.frontend.bank.service;

import com.lro.frontend.bank.model.BankModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final Map<String, List<BalanceSheetRecord>> balanceSheetData = new ConcurrentHashMap<>();

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
            seedBalanceSheet(p.bankId());
            seedLadder(p.bankId(), p.healthStatus());
            recalculateCustomerRelationshipValues(p.bankId());
        }
    }

    private int getCustomerTargetCount(String bankId) {
        return switch (bankId) {
            case "BANK-01-LRG" -> 10000;
            case "BANK-02-MED" -> 7500;
            case "BANK-03-SML" -> 5000;
            case "BANK-04-DST" -> 4000;
            default -> 5000;
        };
    }

    private int getAccountTargetCount(String bankId) {
        return switch (bankId) {
            case "BANK-01-LRG" -> 25000;
            case "BANK-02-MED" -> 15000;
            case "BANK-03-SML" -> 12000;
            case "BANK-04-DST" -> 8000;
            default -> 10000;
        };
    }

    private int getLoanTargetCount(String bankId) {
        return switch (bankId) {
            case "BANK-01-LRG" -> 12000;
            case "BANK-02-MED" -> 8000;
            case "BANK-03-SML" -> 5000;
            case "BANK-04-DST" -> 4000;
            default -> 5000;
        };
    }

    private int getTreasuryTargetCount(String bankId) {
        return switch (bankId) {
            case "BANK-01-LRG" -> 5000;
            case "BANK-02-MED" -> 3000;
            case "BANK-03-SML" -> 2500;
            case "BANK-04-DST" -> 1500;
            default -> 2500;
        };
    }

    private void seedCustomers(String bankId, String health) {
        int targetCount = getCustomerTargetCount(bankId);
        List<CustomerRecord> list = new ArrayList<>(targetCount);

        String[] firstNames = {
            "Aarav", "Priya", "Rohan", "Ananya", "Vikram", "Sunita", "Rajesh", "Sneha", "Aditya", "Pooja",
            "Amit", "Neha", "Deepak", "Kavita", "Suresh", "Meera", "Sanjay", "Divya", "Manoj", "Ritu",
            "Arjun", "Ishita", "Nikhil", "Tanvi", "Rahul", "Swati", "Vivek", "Kiran", "Ashok", "Shweta"
        };
        String[] lastNames = {
            "Sharma", "Nair", "Mehta", "Deshmukh", "Sen", "Patil", "Verma", "Iyer", "Kulkarni", "Gupta",
            "Joshi", "Rao", "Reddy", "Agarwal", "Malhotra", "Singhania", "Bansal", "Choudhury", "Bhattacharya", "Chauhan",
            "Pillai", "Menon", "Trivedi", "Mishra", "Saxena", "Bhatia", "Chatterjee", "Pandey", "Kapoor", "Dhawan"
        };
        String[] smePrefixes = {
            "Omkar", "Ganesh", "Shree", "Sai", "Bharat", "Balaji", "Durga", "Krishna", "Tirupati", "Vardhman",
            "Apex", "Zenith", "Sunrise", "Shakti", "Navkar", "Pinnacle", "Arihant", "Mahalaxmi", "Radhe", "Kuber"
        };
        String[] smeSuffixes = {
            "Logistics Pvt Ltd", "Auto Spares", "Textiles LLP", "Electronics & Tech", "Precision Engg",
            "Impex Solutions", "Agro Commodities", "Steels & Tubes", "Packagings", "Chemicals Ltd"
        };
        String[] corpNames = {
            "Surya Infrastructure Corp", "Titanium Solar Ventures Ltd", "Kaveri Agro Exports Ltd",
            "Apex Petrochem Industries", "Reliance Horizon Global", "Tata Dynamic Technologies",
            "Mahindra Engineering Works", "Larsen Industrial Systems", "Godrej Frontier Logistics",
            "Adani Port & Power Ltd", "Wipro Cloud Infrastructure", "Infosys Digital Labs",
            "Hindalco Smelting Works", "Vedanta Minerals Corp", "JSW Steel Works Ltd"
        };
        String[] branches = {
            "Fort Nariman Point Branch", "MIDC Andheri Branch", "Corporate Finance Branch",
            "Bandra Kurla Complex Branch", "Connaught Place Branch", "MG Road Bangalore Branch",
            "Banjara Hills Hyderabad Branch", "Salt Lake Sector V Branch", "Shivaji Nagar Pune Branch",
            "Ashram Road Ahmedabad Branch"
        };

        for (int id = 1; id <= targetCount; id++) {
            int segmentMod = id % 10;
            String segment;
            String name;
            BigDecimal relVal;
            String risk;
            int accountsCount;

            if (segmentMod < 7) {
                segment = "Retail";
                String fn = firstNames[(id - 1) % firstNames.length];
                String ln = lastNames[((id - 1) / firstNames.length) % lastNames.length];
                name = fn + " " + ln;
                relVal = new BigDecimal(350000 + (id % 50) * 30000).setScale(2, java.math.RoundingMode.HALF_UP);
                risk = "Low";
                accountsCount = 1 + (id % 3);
            } else if (segmentMod < 9) {
                segment = "SME";
                String pfx = smePrefixes[(id - 1) % smePrefixes.length];
                String sfx = smeSuffixes[((id - 1) / smePrefixes.length) % smeSuffixes.length];
                name = pfx + " " + sfx;
                relVal = new BigDecimal(3500000 + (id % 40) * 225000).setScale(2, java.math.RoundingMode.HALF_UP);
                risk = health.equals("Distressed") ? "High" : "Medium";
                accountsCount = 2 + (id % 4);
            } else {
                segment = "Corporate";
                name = corpNames[(id - 1) % corpNames.length];
                if (id > corpNames.length * 10) {
                    name += " Unit " + ((id % 20) + 1);
                }
                relVal = new BigDecimal(45000000 + (id % 25) * 1600000).setScale(2, java.math.RoundingMode.HALF_UP);
                risk = health.equals("Distressed") ? "High" : (id % 2 == 0 ? "Low" : "Medium");
                accountsCount = 3 + (id % 5);
            }

            boolean isDistressedKyc = health.equals("Distressed") && (id % 4 == 0);
            String kyc = (isDistressedKyc || (id % 23 == 0)) ? "Not Verified" : "Verified";

            String loc = switch (id % 4) {
                case 0 -> "Tier 1";
                case 1 -> "Tier 2";
                case 2 -> "Tier 3";
                default -> "Rural";
            };

            String branch = branches[(id - 1) % branches.length];

            list.add(new CustomerRecord(
                    String.format("%s-CUST-%05d", bankId, id),
                    name,
                    segment,
                    kyc,
                    loc,
                    risk,
                    accountsCount,
                    relVal,
                    branch
            ));
        }

        customerData.put(bankId, list);
    }

    private void seedAccounts(String bankId) {
        int targetCount = getAccountTargetCount(bankId);
        List<AccountRecord> list = new ArrayList<>(targetCount);
        List<CustomerRecord> custs = customerData.getOrDefault(bankId, List.of());
        int custCount = custs.isEmpty() ? 1 : custs.size();

        Instant now = Instant.now();
        String[] accountTypes = {"Saving", "Salary", "Current", "Fixed Deposit", "Recurring Deposit", "NRI", "Demat"};

        for (int id = 1; id <= targetCount; id++) {
            CustomerRecord c = custs.get((id - 1) % custCount);
            String accType = accountTypes[(id - 1) % accountTypes.length];

            long baseBal = switch (accType) {
                case "Saving" -> 25000L + ((id * 31L) % 975000L);
                case "Salary" -> 45000L + ((id * 47L) % 455000L);
                case "Current" -> 500000L + ((id * 89L) % 7500000L);
                case "Fixed Deposit" -> 200000L + ((id * 53L) % 4800000L);
                case "Recurring Deposit" -> 50000L + ((id * 19L) % 950000L);
                case "NRI" -> 1500000L + ((id * 71L) % 8500000L);
                case "Demat" -> 10000L + ((id * 37L) % 490000L);
                default -> 150000L;
            };

            BigDecimal bal = BigDecimal.valueOf(baseBal).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal hold = (id % 17 == 0) ? BigDecimal.valueOf(25000L + (id % 10) * 10000L).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String status = (id % 89 == 0) ? "Dormant" : "Active";

            Instant opened = now.minusSeconds((long) (id % 365) * 86400L);

            list.add(new AccountRecord(
                    String.format("%s-ACC-%05d", bankId, id),
                    c.customerId(),
                    accType,
                    "INR",
                    bal,
                    hold,
                    status,
                    opened
            ));
        }

        accountData.put(bankId, list);
    }

    private void seedDeposits(String bankId) {
        int targetCount = 2500;
        List<DepositRecord> list = new ArrayList<>(targetCount);
        List<CustomerRecord> custs = customerData.getOrDefault(bankId, List.of());
        int custCount = custs.isEmpty() ? 1 : custs.size();
        LocalDate today = LocalDate.now();

        String[] instructions = {"Auto-Renew Principal & Interest", "Auto-Renew Principal Only", "Payout to Savings CASA"};

        for (int id = 1; id <= targetCount; id++) {
            CustomerRecord c = custs.get((id - 1) % custCount);
            long princ = 100000L + ((id * 37L) % 4900000L);
            BigDecimal principal = BigDecimal.valueOf(princ).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal rate = new BigDecimal(6.75 + (id % 12) * 0.10).setScale(2, java.math.RoundingMode.HALF_UP);
            LocalDate start = today.minusMonths((id % 12) + 1);
            LocalDate maturity = start.plusMonths(12 + (id % 24));
            BigDecimal matVal = principal.multiply(BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP))).setScale(2, java.math.RoundingMode.HALF_UP);

            list.add(new DepositRecord(
                    String.format("%s-DEP-%05d", bankId, id),
                    c.customerId(),
                    (id % 4 == 0) ? "Recurring Deposit" : "Fixed Deposit",
                    principal,
                    rate,
                    start,
                    maturity,
                    matVal,
                    instructions[id % instructions.length]
            ));
        }

        depositData.put(bankId, list);
    }

    private void seedLoans(String bankId, String health) {
        int targetCount = getLoanTargetCount(bankId);
        List<LoanRecord> list = new ArrayList<>(targetCount);
        List<CustomerRecord> custs = customerData.getOrDefault(bankId, List.of());
        int custCount = custs.isEmpty() ? 1 : custs.size();

        LocalDate today = LocalDate.now();
        String[] loanTypes = {
            "Home Loan", "Mortgage Loan", "Gold Loan", "Vehicle Loan",
            "Loan Against Securities", "Personal Loan", "Education Loan", "Business Loan", "Credit Card"
        };

        for (int id = 1; id <= targetCount; id++) {
            CustomerRecord c = custs.get((id - 1) % custCount);
            String loanType = loanTypes[(id - 1) % loanTypes.length];

            boolean isDistressed = health.equals("Distressed") && (id % 3 == 0);
            int dpd = isDistressed ? (id % 6 == 0 ? 95 : (id % 4 == 0 ? 65 : 35)) : (id % 47 == 0 ? 15 : 0);
            String irac = dpd > 90 ? "Sub-Standard NPA" : (dpd > 60 ? "SMA-2" : (dpd > 30 ? "SMA-1" : (dpd > 0 ? "SMA-0" : "Standard")));

            long baseSanc = switch (loanType) {
                case "Home Loan" -> 3500000L + ((id * 31L) % 6500000L);
                case "Mortgage Loan" -> 5000000L + ((id * 43L) % 8500000L);
                case "Gold Loan" -> 150000L + ((id * 17L) % 850000L);
                case "Vehicle Loan" -> 400000L + ((id * 23L) % 1100000L);
                case "Loan Against Securities" -> 500000L + ((id * 37L) % 2500000L);
                case "Personal Loan" -> 200000L + ((id * 19L) % 800000L);
                case "Education Loan" -> 500000L + ((id * 29L) % 2000000L);
                case "Business Loan" -> 2500000L + ((id * 61L) % 22500000L);
                default -> 100000L + ((id * 13L) % 400000L); // Credit Card
            };

            BigDecimal sanc = BigDecimal.valueOf(baseSanc).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal out = sanc.multiply(BigDecimal.valueOf(0.65 + (id % 30) * 0.01)).setScale(2, java.math.RoundingMode.HALF_UP);

            BigDecimal rate = switch (loanType) {
                case "Home Loan" -> new BigDecimal("8.65");
                case "Mortgage Loan" -> new BigDecimal("9.50");
                case "Gold Loan" -> new BigDecimal("10.25");
                case "Vehicle Loan" -> new BigDecimal("9.15");
                case "Loan Against Securities" -> new BigDecimal("9.75");
                case "Personal Loan" -> new BigDecimal("12.50");
                case "Education Loan" -> new BigDecimal("9.20");
                case "Business Loan" -> new BigDecimal("10.75");
                default -> new BigDecimal("36.00");
            };

            BigDecimal collateral = loanType.equals("Personal Loan") || loanType.equals("Credit Card")
                    ? BigDecimal.ZERO
                    : sanc.multiply(new BigDecimal("1.25")).setScale(2, java.math.RoundingMode.HALF_UP);

            BigDecimal emi = out.divide(new BigDecimal("84"), 2, java.math.RoundingMode.HALF_UP);

            list.add(new LoanRecord(
                    String.format("%s-LN-%05d", bankId, id),
                    c.customerId(),
                    loanType,
                    sanc,
                    out,
                    rate,
                    dpd,
                    irac,
                    collateral,
                    today.plusDays((id % 28) + 1),
                    emi
            ));
        }

        loanData.put(bankId, list);
    }

    private void recalculateCustomerRelationshipValues(String bankId) {
        List<CustomerRecord> custs = customerData.get(bankId);
        if (custs == null || custs.isEmpty()) return;

        Map<String, Integer> accCounts = new HashMap<>();
        Map<String, BigDecimal> accBalances = new HashMap<>();
        for (AccountRecord acc : accountData.getOrDefault(bankId, List.of())) {
            accCounts.merge(acc.customerId(), 1, Integer::sum);
            accBalances.merge(acc.customerId(), acc.availableBalance(), BigDecimal::add);
        }

        Map<String, Integer> loanCounts = new HashMap<>();
        Map<String, BigDecimal> loanBalances = new HashMap<>();
        for (LoanRecord loan : loanData.getOrDefault(bankId, List.of())) {
            loanCounts.merge(loan.customerId(), 1, Integer::sum);
            loanBalances.merge(loan.customerId(), loan.principalOutstanding(), BigDecimal::add);
        }

        List<CustomerRecord> updated = new ArrayList<>(custs.size());
        for (CustomerRecord c : custs) {
            int totalLinked = accCounts.getOrDefault(c.customerId(), 0) + loanCounts.getOrDefault(c.customerId(), 0);
            BigDecimal totalVal = accBalances.getOrDefault(c.customerId(), BigDecimal.ZERO)
                    .add(loanBalances.getOrDefault(c.customerId(), BigDecimal.ZERO));

            updated.add(new CustomerRecord(
                    c.customerId(),
                    c.fullName(),
                    c.segment(),
                    c.kycStatus(),
                    c.locationCategory(),
                    c.riskCategory(),
                    totalLinked,
                    totalVal,
                    c.branchName()
            ));
        }
        customerData.put(bankId, updated);
    }

    private static final String[] TREASURY_CATEGORIES = {
        "Government Bond",
        "Treasury Bill",
        "Corporate Bond",
        "Commercial Paper",
        "Government Securities",
        "Municipal Bond",
        "Mortgage-backed Securities",
        "ETF",
        "Gold commodities"
    };

    private void seedTreasury(String bankId) {
        int targetCount = getTreasuryTargetCount(bankId);
        List<TreasuryHolding> list = new ArrayList<>(targetCount);
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        boolean isDistressed = "BANK-04-DST".equals(bankId);

        String[] gBonds = {"7.18% GS 2033 Benchmark", "7.26% GS 2032", "7.06% GS 2028", "7.38% GS 2027", "6.54% GS 2032", "7.10% GS 2034", "7.40% GS 2035", "6.99% GS 2051", "7.54% GS 2036", "7.17% GS 2028"};
        String[] corpBonds = {"8.15% HDFC Bank AAA Bond 2029", "7.95% ICICI Bank Tier-2 2031", "8.40% REC Limited Bond 2028", "8.20% PFC Infrastructure Bond 2030", "7.85% Bajaj Finance Senior NCD 2027", "8.50% L&T Finance Debenture 2029", "8.65% Tata Capital Bond 2028", "8.10% NTPC PSU Bond 2032"};
        String[] cps = {"Reliance Retail CP 90D", "Tata Motors Finance CP 180D", "Adani Ports CP 90D", "UltraTech Cement CP 60D", "Mahindra & Mahindra Fin CP 120D", "L&T Finance Holdings CP 90D"};
        String[] gSecs = {"7.25% Sovereign Green Bond 2028", "7.30% SDL (Maharashtra) 2033", "7.42% SDL (Gujarat) 2031", "7.35% SDL (Tamil Nadu) 2032", "7.39% SDL (Karnataka) 2034", "7.45% SDL (Uttar Pradesh) 2033"};
        String[] muniBonds = {"8.50% Ahmedabad Municipal Green Bond 2029", "8.40% Pune Municipal Corp Bond 2028", "8.25% Vadodara Municipal Corp 2027", "8.60% Indore Municipal Green Bond 2030", "8.30% Surat Municipal Bond 2029"};
        String[] mbs = {"HDFC Capital RMBS Tranche-A1", "LIC Housing RMBS Trust Series-1", "PNB Housing Prime MBS Class-A", "Can Fin Homes Securitized Note 2028", "Bajaj Housing Finance RMBS 2029"};
        String[] bondEtfs = {"Bharat Bond ETF - April 2030", "Bharat Bond ETF - April 2032", "Nippon India ETF Nifty CPSE Bond", "Edelweiss Nifty PSU Bond Plus SDL ETF", "Mirae Asset Nifty SDL June 2027 ETF"};
        String[] goldCommodities = {"Sovereign Gold Bond 2028 Series-VI", "Sovereign Gold Bond 2029 Series-II", "Nippon India Gold ETF Units", "HDFC Gold ETF Holding", "SBI Gold Sovereign Lot"};

        for (int i = 1; i <= targetCount; i++) {
            String holdingId = bankId + "-TRS-" + String.format("%05d", i);
            String cat = TREASURY_CATEGORIES[(i - 1 + (int)((i * 31L) % 9)) % 9];

            String instName;
            BigDecimal faceValue;
            long qty;
            BigDecimal coupon;
            LocalDate maturity;
            double hairCut;

            switch (cat) {
                case "Government Bond" -> {
                    instName = gBonds[i % gBonds.length];
                    faceValue = new BigDecimal("100.00");
                    qty = 10000L + (long)(Math.abs((i * 1013L) % 4990000));
                    coupon = BigDecimal.valueOf(6.75 + ((i % 125) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(730 + (i % 6000));
                    hairCut = 0.98;
                }
                case "Treasury Bill" -> {
                    int sub = i % 3;
                    instName = (sub == 0 ? "91-Day" : sub == 1 ? "182-Day" : "364-Day") + " T-Bill Series-" + ((i % 40) + 1);
                    faceValue = new BigDecimal("100.00");
                    qty = 50000L + (long)(Math.abs((i * 2017L) % 9950000));
                    coupon = BigDecimal.valueOf(6.40 + ((i % 70) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(sub == 0 ? 15 + (i % 75) : sub == 1 ? 90 + (i % 90) : 180 + (i % 180));
                    hairCut = 0.99;
                }
                case "Corporate Bond" -> {
                    instName = corpBonds[i % corpBonds.length];
                    faceValue = new BigDecimal("1000.00");
                    qty = 5000L + (long)(Math.abs((i * 733L) % 495000));
                    coupon = BigDecimal.valueOf(7.75 + ((i % 150) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(365 + (i % 3650));
                    hairCut = 0.92;
                }
                case "Commercial Paper" -> {
                    instName = cps[i % cps.length];
                    faceValue = new BigDecimal("500000.00");
                    qty = 10L + (long)(Math.abs((i * 97L) % 1990));
                    coupon = BigDecimal.valueOf(7.10 + ((i % 110) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(15 + (i % 250));
                    hairCut = 0.94;
                }
                case "Government Securities" -> {
                    instName = gSecs[i % gSecs.length];
                    faceValue = new BigDecimal("100.00");
                    qty = 10000L + (long)(Math.abs((i * 1237L) % 4990000));
                    coupon = BigDecimal.valueOf(7.20 + ((i % 60) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(730 + (i % 4000));
                    hairCut = 0.97;
                }
                case "Municipal Bond" -> {
                    instName = muniBonds[i % muniBonds.length];
                    faceValue = new BigDecimal("1000.00");
                    qty = 2000L + (long)(Math.abs((i * 449L) % 298000));
                    coupon = BigDecimal.valueOf(8.15 + ((i % 85) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(730 + (i % 2500));
                    hairCut = 0.91;
                }
                case "Mortgage-backed Securities" -> {
                    instName = mbs[i % mbs.length];
                    faceValue = new BigDecimal("10000.00");
                    qty = 500L + (long)(Math.abs((i * 223L) % 49500));
                    coupon = BigDecimal.valueOf(8.40 + ((i % 120) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(1095 + (i % 3000));
                    hairCut = 0.88;
                }
                case "ETF" -> {
                    instName = bondEtfs[i % bondEtfs.length];
                    faceValue = new BigDecimal("1000.00");
                    qty = 10000L + (long)(Math.abs((i * 881L) % 990000));
                    coupon = BigDecimal.valueOf(6.90 + ((i % 90) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    maturity = today.plusDays(730 + (i % 3000));
                    hairCut = 0.95;
                }
                default -> { // Gold commodities
                    instName = goldCommodities[i % goldCommodities.length];
                    faceValue = new BigDecimal("5000.00");
                    qty = 1000L + (long)(Math.abs((i * 359L) % 199000));
                    coupon = (i % 5 <= 1) ? new BigDecimal("2.50") : BigDecimal.ZERO;
                    maturity = (i % 5 <= 1) ? today.plusDays(1460 + (i % 1500)) : null;
                    hairCut = 0.90;
                }
            }

            BigDecimal bookVal = faceValue.multiply(BigDecimal.valueOf(qty))
                    .multiply(BigDecimal.valueOf("Treasury Bill".equals(cat) || "Commercial Paper".equals(cat) ? 0.95 : 1.00))
                    .setScale(2, RoundingMode.HALF_UP);
            double mtmFactor = 0.95 + ((i % 100) / 1000.0);
            BigDecimal marketVal = bookVal.multiply(BigDecimal.valueOf(mtmFactor)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal proceeds = marketVal.multiply(BigDecimal.valueOf(hairCut)).setScale(2, RoundingMode.HALF_UP);

            boolean isEncumbered = isDistressed ? (i % 10 < 5) : (i % 10 < 2);
            String encumbrance = isEncumbered ? "Encumbered (Repo / Collateral)" : "Unencumbered";

            Instant valTime = now.minusSeconds((i % 86400));

            list.add(new TreasuryHolding(
                    holdingId,
                    instName,
                    cat,
                    qty,
                    faceValue,
                    bookVal,
                    marketVal,
                    proceeds,
                    encumbrance,
                    maturity,
                    coupon,
                    valTime
            ));
        }

        treasuryData.put(bankId, list);
    }

    private void seedLedger(String bankId) {
        List<LedgerEntry> list = new ArrayList<>();
        Instant now = Instant.now();

        list.add(new LedgerEntry("LGD-1001", "TXN-CCIL-881", now.minusSeconds(3600), "CASH_RBI_RESERVE", "INTERBANK_CLEARING", new BigDecimal("50000000.00"), BigDecimal.ZERO, "Transfers", "RTGS", "Posted"));
        list.add(new LedgerEntry("LGD-1002", "TXN-DEP-401", now.minusSeconds(7200), "CASA_SAVINGS_LIAB", "CUSTOMER_WITHDRAWAL", BigDecimal.ZERO, new BigDecimal("1500000.00"), "Withdrawal", "Cash", "Posted"));
        list.add(new LedgerEntry("LGD-1003", "TXN-SEC-112", now.minusSeconds(10800), "TREASURY_GSEC_HOLDINGS", "CASH_RBI_RESERVE", new BigDecimal("25000000.00"), BigDecimal.ZERO, "Investment", "NEFT", "Posted"));
        list.add(new LedgerEntry("LGD-1004", "TXN-EMI-992", now.minusSeconds(14400), "LOAN_PORTFOLIO_ASSET", "CASA_SAVINGS_LIAB", BigDecimal.ZERO, new BigDecimal("250000.00"), "Payments", "UPI", "Posted"));

        ledgerData.put(bankId, list);
    }

    private void seedBalanceSheet(String bankId) {
        List<BalanceSheetRecord> list = new ArrayList<>();
        Instant now = Instant.now();
        String pfx = bankId.contains("LRG") ? "LRG" : bankId.contains("MED") ? "MED" : bankId.contains("SML") ? "SML" : "DST";
        BigDecimal baseBal = bankId.contains("LRG") ? new BigDecimal("1950000000000.00")
                : bankId.contains("MED") ? new BigDecimal("790000000000.00")
                : bankId.contains("SML") ? new BigDecimal("410000000000.00") : new BigDecimal("128000000000.00");

        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000001", now.minusSeconds(120), "ACC-" + pfx + "-00101", "Loans & Advances", "Asset", new BigDecimal("2500000.00"), BigDecimal.ZERO, baseBal.add(new BigDecimal("2500000.00")), "Loan Disbursement", "RTGS", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000002", now.minusSeconds(360), "ACC-" + pfx + "-00204", "CASA Deposits", "Liability", BigDecimal.ZERO, new BigDecimal("150000.00"), baseBal.add(new BigDecimal("2650000.00")), "Deposits", "UPI", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000003", now.minusSeconds(900), "ACC-" + pfx + "-00350", "Treasury & G-Sec Investments", "Asset", new BigDecimal("50000000.00"), BigDecimal.ZERO, baseBal.add(new BigDecimal("52650000.00")), "Investment", "RTGS", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000004", now.minusSeconds(1800), "ACC-" + pfx + "-00412", "CASA Deposits", "Liability", new BigDecimal("45000.00"), BigDecimal.ZERO, baseBal.add(new BigDecimal("52605000.00")), "Withdrawal", "IMPS", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000005", now.minusSeconds(3600), "ACC-" + pfx + "-00520", "Loans & Advances", "Asset", BigDecimal.ZERO, new BigDecimal("35000.00"), baseBal.add(new BigDecimal("52570000.00")), "EMI Repayment", "UPI", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000006", now.minusSeconds(7200), "ACC-" + pfx + "-00688", "Term & Fixed Deposits", "Liability", BigDecimal.ZERO, new BigDecimal("1000000.00"), baseBal.add(new BigDecimal("53570000.00")), "Deposits", "NEFT", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000007", now.minusSeconds(10800), "ACC-" + pfx + "-00719", "Cash & RBI Reserves", "Asset", new BigDecimal("15000000.00"), BigDecimal.ZERO, baseBal.add(new BigDecimal("68570000.00")), "Transfers", "RTGS", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000008", now.minusSeconds(14400), "ACC-" + pfx + "-00892", "Interbank Borrowings & Repo", "Liability", BigDecimal.ZERO, new BigDecimal("25000000.00"), baseBal.add(new BigDecimal("93570000.00")), "Interbank Borrowing", "RTGS", "Posted"));
        list.add(new BalanceSheetRecord("BS-" + pfx + "-0000009", now.minusSeconds(21600), "ACC-" + pfx + "-00955", "Statutory Reserves & Retained Surplus", "Equity", BigDecimal.ZERO, new BigDecimal("5000000.00"), baseBal.add(new BigDecimal("98570000.00")), "Reserve Adjustment", "Internal", "Posted"));

        balanceSheetData.put(bankId, list);
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
        return getAccounts(bankId, query, "All");
    }

    @Override
    public List<AccountRecord> getAccounts(String bankId, String query, String accountTypeFilter) {
        List<AccountRecord> all = accountData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(a -> query == null || query.isBlank()
                        || a.accountId().toLowerCase().contains(query.toLowerCase())
                        || a.customerId().toLowerCase().contains(query.toLowerCase()))
                .filter(a -> accountTypeFilter == null || accountTypeFilter.equalsIgnoreCase("All")
                        || a.accountType().equalsIgnoreCase(accountTypeFilter))
                .collect(Collectors.toList());
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
        List<LoanRecord> all = loanData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(l -> query == null || query.isBlank()
                        || l.loanId().toLowerCase().contains(query.toLowerCase())
                        || l.customerId().toLowerCase().contains(query.toLowerCase())
                        || l.loanType().toLowerCase().contains(query.toLowerCase()))
                .filter(l -> npaFilter == null || npaFilter.equalsIgnoreCase("All")
                        || (npaFilter.equalsIgnoreCase("NPA Only") ? l.daysPastDue() > 90 : l.iracClassification().equalsIgnoreCase(npaFilter)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TreasuryHolding> getTreasuryHoldings(String bankId) {
        return getTreasuryHoldings(bankId, "", "All");
    }

    @Override
    public List<TreasuryHolding> getTreasuryHoldings(String bankId, String query, String categoryFilter) {
        List<TreasuryHolding> all = treasuryData.getOrDefault(bankId, List.of());
        return all.stream()
                .filter(t -> query == null || query.isBlank()
                        || t.holdingId().toLowerCase().contains(query.toLowerCase())
                        || t.instrumentName().toLowerCase().contains(query.toLowerCase())
                        || t.category().toLowerCase().contains(query.toLowerCase()))
                .filter(t -> categoryFilter == null || categoryFilter.equalsIgnoreCase("All")
                        || t.category().equalsIgnoreCase(categoryFilter))
                .collect(Collectors.toList());
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
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId) {
        return balanceSheetData.getOrDefault(bankId, List.of());
    }

    @Override
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId, String query, String categoryFilter) {
        return getBalanceSheetRecords(bankId, query, categoryFilter, 2000);
    }

    @Override
    public List<BalanceSheetRecord> getBalanceSheetRecords(String bankId, String query, String categoryFilter, int limit) {
        List<BalanceSheetRecord> all = balanceSheetData.getOrDefault(bankId, List.of());
        boolean isAllCategories = categoryFilter == null || categoryFilter.isBlank() || categoryFilter.trim().toLowerCase().startsWith("all");
        return all.stream()
                .filter(r -> isAllCategories || r.category().equalsIgnoreCase(categoryFilter.trim()))
                .filter(r -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase().trim();
                    return r.recordId().toLowerCase().contains(q)
                            || r.accountReference().toLowerCase().contains(q)
                            || r.lineItem().toLowerCase().contains(q)
                            || r.type().toLowerCase().contains(q)
                            || r.method().toLowerCase().contains(q);
                })
                .limit(limit > 0 ? limit : 2000)
                .collect(Collectors.toList());
    }

    @Override
    public BalanceSheetSummary getBalanceSheetSummary(String bankId) {
        BankProfile p = getBankProfile(bankId).orElse(null);
        if (p == null) {
            return new BalanceSheetSummary(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L
            );
        }

        BigDecimal totalAssets = p.totalAssets();
        BigDecimal totalLiabilities = p.totalLiabilities();
        BigDecimal totalEquity = totalAssets.subtract(totalLiabilities);

        BigDecimal casaDeposits = p.totalDeposits().multiply(new BigDecimal("0.55")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal termDeposits = p.totalDeposits().subtract(casaDeposits);
        BigDecimal nonDepositLiab = totalLiabilities.subtract(p.totalDeposits());
        BigDecimal interbankBorrowings = nonDepositLiab.multiply(new BigDecimal("0.65")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherLiabilities = nonDepositLiab.subtract(interbankBorrowings);

        BigDecimal loansAndAdvances = p.totalLoans();
        BigDecimal treasuryInvestments = p.securitiesHoldings();
        BigDecimal cashAndRbiReserves = p.cashBalance();
        BigDecimal otherAssets = totalAssets.subtract(loansAndAdvances).subtract(treasuryInvestments).subtract(cashAndRbiReserves);

        BigDecimal shareCapital = totalEquity.multiply(new BigDecimal("0.35")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal reservesAndSurplus = totalEquity.subtract(shareCapital);

        long txnCount = switch (bankId) {
            case "BANK-01-LRG" -> 1000000L;
            case "BANK-02-MED" -> 500000L;
            case "BANK-03-SML" -> 300000L;
            case "BANK-04-DST" -> 180000L;
            default -> 100000L;
        };

        return new BalanceSheetSummary(
                totalAssets,
                totalLiabilities,
                totalEquity,
                totalEquity,
                true,
                BigDecimal.ZERO,
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
                txnCount
        );
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
                type.equalsIgnoreCase("Deposit") ? "Deposits" : "Withdrawal",
                "Cash",
                "Posted"
        ));
        return true;
    }

    @Override
    public boolean originateDemoLoan(String bankId, String customerId, String loanType, BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        String newLoanId = String.format("%s-LN-%05d", bankId, (loanData.getOrDefault(bankId, List.of()).size() + 1));
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
                "Payments",
                "UPI",
                "Posted"
        ));
        return true;
    }
}
