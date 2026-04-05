package com.act.json.model;

import java.math.BigDecimal;

public class EventAction {

    String fromLedgerName;
    String toLedgerName;
    String type; //amountRatePerHour or fixedAmount or source
    BigDecimal amountRatePerHour = BigDecimal.ZERO;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



    public BigDecimal getAmountRatePerHour() {
        return amountRatePerHour;
    }

    public void setAmountRatePerHour(BigDecimal amountRatePerHour) {
        this.amountRatePerHour = amountRatePerHour;
    }

    public String getToLedgerName() {
        return toLedgerName;
    }

    public void setToLedgerName(String toLedgerName) {
        this.toLedgerName = toLedgerName;
    }

    public String getFromLedgerName() {
        return fromLedgerName;
    }

    public void setFromLedgerName(String fromLedgerName) {
        this.fromLedgerName = fromLedgerName;
    }





}
