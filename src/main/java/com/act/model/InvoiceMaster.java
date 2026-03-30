package com.act.model;


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
    private LocalDate invoiceDate;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;


    @OneToMany(mappedBy = "invoiceMaster", cascade = CascadeType.ALL)
    private List<InvoiceDetail> details;

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

}
