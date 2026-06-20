package org.example.bookinghotels.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSequenceService {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSequenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void synchronize(String tableName) {
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ");
        }
        String sequence = jdbcTemplate.queryForObject(
                "select pg_get_serial_sequence(?, 'id')", String.class, tableName);
        if (sequence == null || sequence.isBlank()) {
            return;
        }
        String sql = "select setval(?::regclass, coalesce((select max(id) from " + tableName + "), 0) + 1, false)";
        jdbcTemplate.queryForObject(sql, Long.class, sequence);
    }
}
