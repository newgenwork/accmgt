package com.act.repo;


import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<TimeSheet, Long> {


    @Query(
            "SELECT t FROM TimeSheet t " +
                    "WHERE t.employee = :employee " +
                    "AND (:id IS NULL OR t.id <> :id) " +
                    "AND t.startDate <= :endDate " +
                    "AND t.endDate >= :startDate"
    )
    List<TimeSheet> findCollidingTimeSheets(
            @Param("employee") Ledger employee,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("id") Long id
    );


    @Query(
            "SELECT COALESCE(SUM(t.noOfHrs), 0) " +
                    "FROM TimeSheet t " +
                    "WHERE t.employee.id = :employeeId " +
                    "AND t.invoiceDetail is  null " +
                    "AND t.startDate >= :startDate " +
                    "AND t.endDate <= :endDate"
    )
    BigDecimal getTotalHoursByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
