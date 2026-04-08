package com.act.repo;


import com.act.model.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    Optional<Ledger> findByLedgerName(String ledgerName);

    Optional<List<Ledger>> findByIsEmployeeAndType(String isEmployee, String type);
    Optional<List<Ledger>> findByIsEmployeeAndTypeAndLabel(String isEmployee, String type, String label);

    Optional<List<Ledger>> findByType(String type);

    Optional<List<Ledger>> findByIsEmployeeAndTypeAndInvoiceLedger(String isEmployee, String type ,Ledger invoiceLedger);


}
