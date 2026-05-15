package com.act.repo;

import com.act.model.Ledger;
import com.act.model.LedgerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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



        @Query("SELECT d FROM LedgerDocument d WHERE d.expiryDate BETWEEN :today" +
                " AND :fourMonthsLater AND " +
                " (d.ledger.invoiceRateValidateToDate IS NULL OR d.ledger.invoiceRateValidateToDate >= :today) " +
                " ORDER BY d.expiryDate ASC")
        List<LedgerDocument> findExpiringActiveDocs(
                @Param("today") LocalDate today,
                @Param("fourMonthsLater") LocalDate fourMonthsLater
        );

}
