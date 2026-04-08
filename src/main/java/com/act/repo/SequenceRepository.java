package com.act.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SequenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public SequenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long getNextInvoiceSequence() {
        return jdbcTemplate.queryForObject(
                "SELECT nextval('invoice_seq')",
                Long.class
        );
    }
}