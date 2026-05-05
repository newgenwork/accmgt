package com.act.repo;

import com.act.model.Ledger;
import com.act.model.LedgerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LedgerDocumentRepository  extends JpaRepository<LedgerDocument, Long> {



        /**
         * Get all important dates for a ledger ordered by date
         */
        List<LedgerDocument> findByLedgerOrderByExpiryDateAsc(Ledger ledger);

        /**
         * (Optional) Get upcoming dates from today
         */
        List<LedgerDocument> findByLedgerAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(
                Ledger ledger,
                LocalDate date
        );

}
