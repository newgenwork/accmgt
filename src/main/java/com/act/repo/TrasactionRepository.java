package com.act.repo;


import com.act.model.InvoiceMaster;
import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrasactionRepository extends JpaRepository<Transaction, Long> {

    Optional<List<Transaction>> findByJournalEntry(JournalEntry journalEntry);
    Optional<List<Transaction>> findByInvoiceMaster(InvoiceMaster invoiceMaster);
    Optional<List<Transaction>> findByAccount(Ledger account);
}
