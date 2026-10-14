package com.lro.frontend.bank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class BankModels {

    public record BankProfile(
            String bankId,
            String bankName,
            String tierDescription,
            long customerCount,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal totalDeposits,
            BigDecimal totalLoans,
            BigDecimal cashBalance,
            BigDecimal securitiesHoldings,
            BigDecimal grossNpaRatio,
            BigDecimal netInterestMargin,
            String healthStatus, // "Healthy", "Moderate", "Distressed"
            Instant lastDataRefresh
    ) {}

    public record CustomerRecord(
            String customerId,
            String fullName,
            String segment, // "Retail", "SME", "Corporate"
            String kycStatus, // "Verified", "Not Verified"
            String locationCategory, // "Tier 1", "Tier 2", "Tier 3", "Rural"
            String riskCategory, // "Low", "Medium", "High"
            int activeAccountsCount,
            BigDecimal totalRelationshipValue,
            String branchName
    ) {}

    public record AccountRecord(
            String accountId,
            String customerId,
            String accountType, // "Savings CASA", "Current CASA", "Corporate Current"
            String currency, // "INR"
            BigDecimal availableBalance,
            BigDecimal holdAmount,
            String status, // "Active", "Dormant", "Frozen"
            Instant openedDate
    ) {}

    public record DepositRecord(
            String depositId,
            String customerId,
            String productType, // "Fixed Deposit", "Recurring Deposit", "Tax Saver FD"
            BigDecimal principalAmount,
            BigDecimal interestRatePercent,
            LocalDate startDate,
            LocalDate maturityDate,
            BigDecimal maturityAmount,
            String renewalInstruction // "Auto-Renew Principal & Interest", "Credit to Savings", "Payout Monthly"
    ) {}

    public record LoanRecord(
            String loanId,
            String customerId,
            String loanType, // "Home Loan", "Working Capital Loan", "MSME Term Loan", "Auto Loan"
            BigDecimal sanctionedAmount,
            BigDecimal principalOutstanding,
            BigDecimal interestRatePercent,
            int daysPastDue,
            String iracClassification, // "Standard", "SMA-0", "SMA-1", "SMA-2", "Sub-Standard NPA", "Doubtful NPA", "Loss"
            BigDecimal collateralValue,
            LocalDate nextEmiDueDate,
            BigDecimal monthlyEmi
    ) {}

    public record TreasuryHolding(
            String holdingId,
            String instrumentName,
            String category, // "G-Sec", "T-Bill", "Corporate Bond", "Equity"
            long quantity,
            BigDecimal faceValuePerUnit,
            BigDecimal bookValue,
            BigDecimal simulatedMarketValue,
            BigDecimal estimatedLiquidationProceeds,
            String encumbranceStatus, // "Unencumbered (Free)", "Encumbered (Repo / Collateral)"
            LocalDate maturityDate,
            BigDecimal couponOrYieldPercent,
            Instant lastValuationTime
    ) {}

    public record LedgerEntry(
            String entryId,
            String transactionRef,
            Instant timestamp,
            String accountReference,
            String counterpartyRef,
            BigDecimal debitINR,
            BigDecimal creditINR,
            String narration,
            String status // "Posted", "Pending", "Reversed"
    ) {}

    public record CashFlowBucket(
            String tenorBucket, // "Same Day", "2-7 Days", "8-30 Days", "31-90 Days", ">90 Days"
            BigDecimal contractualInflows,
            BigDecimal contractualOutflows,
            BigDecimal netFundingGap,
            BigDecimal cumulativeFundingGap
    ) {}
}
