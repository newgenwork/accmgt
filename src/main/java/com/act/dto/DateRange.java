package com.act.dto;

import java.time.LocalDate;

public class DateRange {

    private LocalDate startDate;
    private LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }


    @Override
    public java.lang.String toString() {
        return startDate.toString() + " - " + endDate.toString();
    }
}