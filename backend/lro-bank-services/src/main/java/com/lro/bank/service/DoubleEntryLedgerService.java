package com.lro.bank.service;

import com.lro.common.exception.ValidationException;
import com.lro.common.model.Money;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Double-Entry Banking General Ledger Service implementing Prompt 14.
 * Guarantees strict transactional debit/credit balancing invariants.
 */
public class DoubleEntryLedgerService {

    public record JournalEntry(
            String entryId,
            String transactionRef,
            Instant timestamp,
            String accountRef,
            Money debit,
            Money credit,
            String narration,
            String status
    ) {}

    public record TransactionBatch(
            String transactionRef,
            Instant timestamp,
            String narration,
            List<JournalEntry> entries
    ) {}

    private final Map<String, List<JournalEntry>> ledgerByBank = new ConcurrentHashMap<>();

    public TransactionBatch postTransaction(String bankId, String transactionRef, String narration, List<JournalEntry> entries) {
        Objects.requireNonNull(bankId, "bankId cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");

        if (entries.isEmpty()) {
            throw new ValidationException("Transaction batch cannot be empty");
        }

        // Validate double-entry invariant: sum(debit) == sum(credit)
        Money totalDebit = Money.ZERO;
        Money totalCredit = Money.ZERO;

        for (JournalEntry entry : entries) {
            totalDebit = totalDebit.add(entry.debit());
            totalCredit = totalCredit.add(entry.credit());
        }

        if (!totalDebit.equals(totalCredit)) {
            throw new ValidationException("Double-entry ledger imbalance: Total Debit (" + totalDebit + ") != Total Credit (" + totalCredit + ")");
        }

        List<JournalEntry> bankLedger = ledgerByBank.computeIfAbsent(bankId, k -> new CopyOnWriteArrayList<>());
        bankLedger.addAll(entries);

        return new TransactionBatch(transactionRef, Instant.now(), narration, entries);
    }

    public List<JournalEntry> getLedgerEntries(String bankId) {
        return Collections.unmodifiableList(ledgerByBank.getOrDefault(bankId, List.of()));
    }

    public Map<String, Money> getTrialBalance(String bankId) {
        Map<String, Money> balances = new HashMap<>();
        List<JournalEntry> entries = ledgerByBank.getOrDefault(bankId, List.of());

        for (JournalEntry e : entries) {
            Money current = balances.getOrDefault(e.accountRef(), Money.ZERO);
            // Balance = Debits - Credits
            balances.put(e.accountRef(), current.add(e.debit()).subtract(e.credit()));
        }
        return balances;
    }
}
