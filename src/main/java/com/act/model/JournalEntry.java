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
    @JoinColumn(name = "fromVendor", nullable = false)
    private Ledger fromVendor;
    @ManyToOne
    @JoinColumn(name = "toVendor", nullable = false)
    private Ledger toVendor;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate transactionDate;
}
