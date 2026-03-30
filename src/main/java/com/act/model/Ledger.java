package com.act.model;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "ledg_act", uniqueConstraints = @UniqueConstraint(columnNames = "ledgerName"))
public class Ledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "ledgerName")
    private String ledgerName;
    @Column(name = "config")
    private String config;
    @Column(name = "enable")
    private String enable;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false)
    private String isEmployee; //true or false
    @Column(nullable = false)
    private String type; //Equity  or liability or Asset or Expense
    @Column(nullable = false)
    private LocalDateTime balanceUpdateDate;


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
}
