package com.act.repo;


import com.act.model.Client;
import com.act.model.JournalEntry;
import com.act.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {


}
