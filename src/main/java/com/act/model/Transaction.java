package com.act.model;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_act")
public class Transaction {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account", nullable = false)
    private Ledger account;


    @ManyToOne
    @JoinColumn(name = "journalEntry", nullable = true)
    private JournalEntry journalEntry;

    @ManyToOne
    @JoinColumn(name = "invoiceMaster", nullable = true)
    private InvoiceMaster invoiceMaster;

    @ManyToOne
    @JoinColumn(name = "invoiceDetail", nullable = true)
    private InvoiceDetail invoiceDetail;

    @Column(nullable = true)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ledger getAccount() {
        return account;
    }

    public void setAccount(Ledger account) {
        this.account = account;
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

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public InvoiceMaster getInvoiceMaster() {
        return invoiceMaster;
    }

    public void setInvoiceMaster(InvoiceMaster invoiceMaster) {
        this.invoiceMaster = invoiceMaster;
    }

    public InvoiceDetail getInvoiceDetail() {
        return invoiceDetail;
    }

    public void setInvoiceDetail(InvoiceDetail invoiceDetail) {
        this.invoiceDetail = invoiceDetail;
    }
}
