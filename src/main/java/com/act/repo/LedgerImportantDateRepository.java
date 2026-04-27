package com.act.repo;

import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.LedgerImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LedgerImportantDateRepository  extends JpaRepository<LedgerImportantDate, Long> {



        /**
         * Get all important dates for a ledger ordered by date
         */
        List<LedgerImportantDate> findByLedgerOrderByImportantDateAsc(Ledger ledger);

        /**
         * (Optional) Get upcoming dates from today
         */
        List<LedgerImportantDate> findByLedgerAndImportantDateGreaterThanEqualOrderByImportantDateAsc(
                Ledger ledger,
                LocalDate date
        );

}
