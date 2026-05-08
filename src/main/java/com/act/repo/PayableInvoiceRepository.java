package com.act.repo;

import com.act.model.PayableInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableInvoiceRepository
        extends JpaRepository<PayableInvoice, Long> {
}