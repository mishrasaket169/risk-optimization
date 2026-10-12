package com.lro.frontend.bank.service;

import com.lro.frontend.bank.model.BankModels.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BankService {
    List<BankProfile> getAllBankProfiles();
    Optional<BankProfile> getBankProfile(String bankId);
    List<CustomerRecord> getCustomers(String bankId, String query, String segmentFilter);
    List<AccountRecord> getAccounts(String bankId, String query);
    default List<AccountRecord> getAccounts(String bankId, String query, String accountTypeFilter) {
        return getAccounts(bankId, query);
    }
    List<DepositRecord> getDeposits(String bankId);
    default List<DepositRecord> getDeposits(String bankId, String query) {
        return getDeposits(bankId);
    }
    List<LoanRecord> getLoans(String bankId, String npaFilter);
    default List<LoanRecord> getLoans(String bankId, String query, String npaFilter) {
        return getLoans(bankId, npaFilter);
    }
    List<TreasuryHolding> getTreasuryHoldings(String bankId);
    default List<TreasuryHolding> getTreasuryHoldings(String bankId, String query, String categoryFilter) {
        return getTreasuryHoldings(bankId);
    }
    List<LedgerEntry> getLedgerEntries(String bankId);
    List<CashFlowBucket> getCashFlowLadder(String bankId);

    // Synthetic operational actions (demo previews)
    boolean openSyntheticAccount(String bankId, String customerId, String accountType, BigDecimal initialDeposit);
    boolean recordDepositTransaction(String bankId, String accountId, BigDecimal amount, String type);
    boolean originateDemoLoan(String bankId, String customerId, String loanType, BigDecimal amount, BigDecimal rate);
    boolean recordLoanRepayment(String bankId, String loanId, BigDecimal emiAmount);
}
