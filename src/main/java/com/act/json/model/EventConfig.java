package com.act.json.model;

import java.time.LocalDate;
import java.util.List;

public class EventConfig {
    LocalDate validFrom;


    LocalDate validTo;
    List<EventAction> eventAction;

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public List<EventAction> getEventAction() {
        return eventAction;
    }

    public void setEventAction(List<EventAction> action) {
        this.eventAction = action;
    }
}
