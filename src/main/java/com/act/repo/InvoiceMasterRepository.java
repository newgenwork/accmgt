package com.act.repo;


import com.act.model.Ledger;
import com.act.model.InvoiceMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceMasterRepository extends JpaRepository<InvoiceMaster, Long> {

    Optional<InvoiceMaster> findByReference(String reference);

}
