package com.act.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "journal_entry_act")
public class JournalEntry {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "fromAccount", nullable = false)
    private Ledger fromAccount;
    @ManyToOne
    @JoinColumn(name = "toAccount", nullable = false)
    private Ledger toAccount;

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

    @Column(nullable = false)
    private String type; // Non Client Billable Hrs Payment to AP , Release Payment to Vendor/Candidate

    @Column(nullable = true)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ledger getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Ledger fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Ledger getToAccount() {
        return toAccount;
    }

    public void setToAccount(Ledger toAccount) {
        this.toAccount = toAccount;
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
}
