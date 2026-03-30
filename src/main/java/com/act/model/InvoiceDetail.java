package com.act.model;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_detail_act")
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_master_id", nullable = false)
    private InvoiceMaster invoiceMaster;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer noOfHrs;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    public InvoiceDetail() {}

    public InvoiceDetail(Long id, InvoiceMaster invoiceMaster, Long employeeId,
                         Integer noOfHrs, BigDecimal rate, BigDecimal amount) {

        this.id = id;
        this.invoiceMaster = invoiceMaster;
        this.employeeId = employeeId;
        this.noOfHrs = noOfHrs;
        this.rate = rate;
        this.amount = amount;
    }

    // -------- Getters & Setters -------- //

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public InvoiceMaster getInvoiceMaster() { return invoiceMaster; }

    public void setInvoiceMaster(InvoiceMaster invoiceMaster) { this.invoiceMaster = invoiceMaster; }

    public Long getEmployeeId() { return employeeId; }

    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Integer getNoOfHrs() { return noOfHrs; }

    public void setNoOfHrs(Integer noOfHrs) { this.noOfHrs = noOfHrs; }

    public BigDecimal getRate() { return rate; }

    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
}