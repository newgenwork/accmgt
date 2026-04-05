package com.act.dto;

import com.act.model.InvoiceMaster;
import com.act.model.JournalEntry;
import com.act.model.Ledger;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDto {

    private String details;

    private String description;

    private BigDecimal amount;

    private BigDecimal balance;


    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;


    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
