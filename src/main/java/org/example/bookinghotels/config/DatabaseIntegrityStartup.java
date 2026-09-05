package org.example.bookinghotels.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sửa các sequence PostgreSQL bị lệch do dữ liệu cũ được import bằng ID thủ công.
 * Đồng thời giữ lại hóa đơn mồ côi nhưng bỏ liên kết booking không còn tồn tại,
 * để Hibernate có thể tạo lại khóa ngoại invoices.booking_id -> bookings.id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseIntegrityStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIntegrityStartup.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseIntegrityStartup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        repairSequence("room", "id");
        repairSequence("room_type", "id");
        repairSequence("bookings", "id");
        repairSequence("booking_detail", "id");
        repairSequence("favorite_room", "id");
        repairSequence("invoices", "id");
        detachOrphanInvoices();
    }

    private void repairSequence(String table, String idColumn) {
        try {
            String sql = "SELECT setval(" +
                    "pg_get_serial_sequence('" + table + "', '" + idColumn + "'), " +
                    "COALESCE((SELECT MAX(" + idColumn + ") FROM " + table + "), 0) + 1, false)";
            jdbcTemplate.queryForObject(sql, Long.class);
            log.info("Đã đồng bộ sequence cho bảng {}", table);
        } catch (Exception ex) {
            // Không chặn ứng dụng nếu bảng chưa tồn tại hoặc cột không dùng sequence.
            log.debug("Bỏ qua đồng bộ sequence {}: {}", table, ex.getMessage());
        }
    }

    private void detachOrphanInvoices() {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = current_schema() AND table_name = 'invoices'",
                    Integer.class);
            if (exists == null || exists == 0) return;

            int updated = jdbcTemplate.update(
                    "UPDATE invoices i SET booking_id = NULL " +
                            "WHERE i.booking_id IS NOT NULL " +
                            "AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.id = i.booking_id)");
            if (updated > 0) {
                log.warn("Đã tách {} hóa đơn khỏi booking không còn tồn tại; dữ liệu hóa đơn vẫn được giữ nguyên", updated);
            }
        } catch (Exception ex) {
            log.debug("Bỏ qua sửa hóa đơn mồ côi: {}", ex.getMessage());
        }
    }
}
