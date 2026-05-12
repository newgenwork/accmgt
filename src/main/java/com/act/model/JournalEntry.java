package com.act.model;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "journal_entry_act")
public class JournalEntry {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "targetAccount", nullable = false)
    private Ledger targetAccount;


    @Column(nullable = false)
    private String type; // Non Client Billable Hrs Payment to AP , Release Payment to Vendor/Candidate

    @Column(nullable = true)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_invoice_id")
    private PayableInvoice payableInvoice;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Ledger getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Ledger targetAccount) {
        this.targetAccount = targetAccount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public PayableInvoice getPayableInvoice() {
        return payableInvoice;
    }

    public void setPayableInvoice(PayableInvoice payableInvoice) {
        this.payableInvoice = payableInvoice;
    }
}
