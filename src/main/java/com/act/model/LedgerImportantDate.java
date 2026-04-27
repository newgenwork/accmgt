package com.act.model;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "ledger_important_date")
public class LedgerImportantDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false)
    private LocalDate importantDate;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 50)
    private String type; // OPTIONAL (e.g., CONTRACT, RENEWAL, COMPLIANCE)

    // getters & setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public LocalDate getImportantDate() {
        return importantDate;
    }

    public void setImportantDate(LocalDate importantDate) {
        this.importantDate = importantDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
