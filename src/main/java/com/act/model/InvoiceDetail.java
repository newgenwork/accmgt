package com.act.model;


import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = "invoice_detail_act")
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_master_id", nullable = false)
    private InvoiceMaster invoiceMaster;

    // ✅ Employee as Ledger
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_ledger_id", nullable = false)
    private Ledger employee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal noOfHrs;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(nullable = true)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InvoiceMaster getInvoiceMaster() {
        return invoiceMaster;
    }

    public void setInvoiceMaster(InvoiceMaster invoiceMaster) {
        this.invoiceMaster = invoiceMaster;
    }

    public Ledger getEmployee() {
        return employee;
    }

    public void setEmployee(Ledger employee) {
        this.employee = employee;
    }

    public BigDecimal getNoOfHrs() {
        return noOfHrs;
    }

    public void setNoOfHrs(BigDecimal noOfHrs) {
        this.noOfHrs = noOfHrs;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Getters & Setters
}
