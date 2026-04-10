package com.act.model;


import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Entity
@Table(name = "invoice_master_act")
public class InvoiceMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;


    @Column(nullable = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;

    @Column(nullable = false)
    private String status;   //DRAFT, SUBMITTED, PAID

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Ledger client;


    @OneToMany(mappedBy = "invoiceMaster", cascade = CascadeType.ALL , orphanRemoval = true)
    private List<InvoiceDetail> details;

    @Column(nullable = true)
    private String notes;

    public InvoiceMaster() {}

    public InvoiceMaster(Long id, String reference, LocalDate invoiceDate, List<InvoiceDetail> details) {
        this.id = id;
        this.reference = reference;
        this.invoiceDate = invoiceDate;
        this.details = details;
    }

    // -------- Getters & Setters -------- //

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }

    public void setReference(String reference) { this.reference = reference; }

    public LocalDate getInvoiceDate() { return invoiceDate; }

    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public List<InvoiceDetail> getDetails() { return details; }

    public void setDetails(List<InvoiceDetail> details) { this.details = details; }
    public Ledger getClient() {
        return client;
    }

    public void setClient(Ledger client) {
        this.client = client;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
