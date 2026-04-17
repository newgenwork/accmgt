package com.act.dto;


import com.act.model.Ledger;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class LedgerDto {
    private Long id;


    private String ledgerName;
    private String config;
    private String shareConfig;
    private String enable;
    private BigDecimal balance;
    private String isEmployee; //true or false
    private String isJournalEntryPossible; //true or false

    private String type; //Equity  or liability or Asset or Expense
    private LocalDateTime balanceUpdateDate;
    private BigDecimal companyHourRate;
    private BigDecimal invoiceRate;
    @DateTimeFormat(pattern = "MM-dd-YYYY")
    private LocalDate invoiceRateValidateFromDate;

    @DateTimeFormat(pattern = "MM-dd-YYYY")
    private LocalDate invoiceRateValidateToDate;

    private Ledger invoiceLedger;

    private String label;
    private String companyName;
    private String companyAddress;

    private String invoiceCreationType ; //batch or consolidated

    private String missingTimsheet;

    private String endClientName ;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }




    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }


    public LocalDateTime getBalanceUpdateDate() {
        return balanceUpdateDate;
    }

    public void setBalanceUpdateDate(LocalDateTime balanceUpdateDate) {
        this.balanceUpdateDate = balanceUpdateDate;
    }

    public String getLedgerName() {
        return ledgerName;
    }

    public void setLedgerName(String ledgerName) {
        this.ledgerName = ledgerName;
    }

    public String getIsEmployee() {
        return isEmployee;
    }

    public void setIsEmployee(String isEmployee) {
        this.isEmployee = isEmployee;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public BigDecimal getInvoiceRate() {
        return invoiceRate;
    }

    public void setInvoiceRate(BigDecimal invoiceRate) {
        this.invoiceRate = invoiceRate;
    }

    public LocalDate getInvoiceRateValidateFromDate() {
        return invoiceRateValidateFromDate;
    }

    public void setInvoiceRateValidateFromDate(LocalDate invoiceRateValidateFromDate) {
        this.invoiceRateValidateFromDate = invoiceRateValidateFromDate;
    }

    public LocalDate getInvoiceRateValidateToDate() {
        return invoiceRateValidateToDate;
    }

    public void setInvoiceRateValidateToDate(LocalDate invoiceRateValidateToDate) {
        this.invoiceRateValidateToDate = invoiceRateValidateToDate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public Ledger getInvoiceLedger() {
        return invoiceLedger;
    }

    public void setInvoiceLedger(Ledger invoiceLedger) {
        this.invoiceLedger = invoiceLedger;
    }

    public String getInvoiceCreationType() {
        return invoiceCreationType;
    }

    public void setInvoiceCreationType(String invoiceCreationType) {
        this.invoiceCreationType = invoiceCreationType;
    }

    public String getShareConfig() {
        return shareConfig;
    }

    public void setShareConfig(String shareConfig) {
        this.shareConfig = shareConfig;
    }

    public String getIsJournalEntryPossible() {
        return isJournalEntryPossible;
    }

    public void setIsJournalEntryPossible(String isJournalEntryPossible) {
        this.isJournalEntryPossible = isJournalEntryPossible;
    }

    public String getMissingTimsheet() {
        return missingTimsheet;
    }

    public void setMissingTimsheet(String missingTimsheet) {
        this.missingTimsheet = missingTimsheet;
    }

    public BigDecimal getCompanyHourRate() {
        return companyHourRate;
    }

    public void setCompanyHourRate(BigDecimal companyHourRate) {
        this.companyHourRate = companyHourRate;
    }

    public String getEndClientName() {
        return endClientName;
    }

    public void setEndClientName(String endClientName) {
        this.endClientName = endClientName;
    }
}
