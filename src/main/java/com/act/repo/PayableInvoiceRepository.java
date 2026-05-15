package com.act.repo;

import com.act.model.Ledger;
import com.act.model.PayableInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayableInvoiceRepository
        extends JpaRepository<PayableInvoice, Long> {

    List<PayableInvoice> findByStatus(String status);
    List<PayableInvoice> findByVendorAndStatus(Ledger vendor, String status);
}