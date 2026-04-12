package com.act.repo;


import com.act.model.JournalEntry;
import com.act.model.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetRepository extends JpaRepository<TimeSheet, Long> {


}
