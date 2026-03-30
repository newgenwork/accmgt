package com.act.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "journal_entry_act")
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fromVendor", nullable = false)
    private Vendor fromVendor;

    @ManyToOne
    @JoinColumn(name = "toVendor", nullable = false)
    private Vendor toVendor;

    @Column(nullable = false)
    private LocalDate transactionDate;
}
