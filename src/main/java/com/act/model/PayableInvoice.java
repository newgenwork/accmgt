package com.act.model;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payable_invoice_act")
public class PayableInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    Ledger vendor;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceReceiveDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference" )
    private String reference;

    @Column(name = "description" )
    private String description;

    @Column(name = "notes" , length = 2000)
    private String notes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "document_content", nullable = false)
    private byte[] documentContent;


    @Column(length = 255)
    private String fileName;

    @Column(length = 100)
    private String contentType;


    @Column(length = 100)
    private String status; //SUBMITTED or PAID or DECLINED

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ledger getVendor() {
        return vendor;
    }

    public void setVendor(Ledger vendor) {
        this.vendor = vendor;
    }

    public LocalDate getInvoiceReceiveDate() {
        return invoiceReceiveDate;
    }

    public void setInvoiceReceiveDate(LocalDate invoiceReceiveDate) {
        this.invoiceReceiveDate = invoiceReceiveDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public byte[] getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(byte[] documentContent) {
        this.documentContent = documentContent;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean hasFile() {
        return documentContent != null && documentContent.length>0;
    }

}
