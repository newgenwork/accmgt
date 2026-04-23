package com.act.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TimesheetFilter {

    private Long clientId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate ;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate ;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
