package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<Invoices, Long> {

    Optional<Invoices> findFirstByBookingIdOrderByIdDesc(Long bookingId);

    // ==== Bản không lọc (StatisticsController đang dùng) ====

    @Query(value = """
            SELECT TO_CHAR(invoice_date,'YYYY-MM-DD'),
                   SUM(total_amount)
            FROM invoices
            GROUP BY TO_CHAR(invoice_date,'YYYY-MM-DD')
            ORDER BY TO_CHAR(invoice_date,'YYYY-MM-DD')
            """, nativeQuery = true)
    List<Object[]> getRevenueByDay();

    @Query(value = """
            SELECT TO_CHAR(invoice_date,'YYYY-MM'),
                   SUM(total_amount)
            FROM invoices
            GROUP BY TO_CHAR(invoice_date,'YYYY-MM')
            ORDER BY TO_CHAR(invoice_date,'YYYY-MM')
            """, nativeQuery = true)
    List<Object[]> getRevenueByMonth();

    @Query(value = """
            SELECT TO_CHAR(invoice_date,'YYYY'),
                   SUM(total_amount)
            FROM invoices
            GROUP BY TO_CHAR(invoice_date,'YYYY')
            ORDER BY TO_CHAR(invoice_date,'YYYY')
            """, nativeQuery = true)
    List<Object[]> getRevenueByYear();

    // ==== Bản có lọc theo khoảng ngày / theo tháng-năm ====
    // Chú ý: mọi tham số đều CAST tường minh vì PostgreSQL không tự suy
    // được kiểu dữ liệu khi tham số chỉ xuất hiện trong "... IS NULL".

    @Query(value = """
            SELECT TO_CHAR(invoice_date,'YYYY-MM-DD'),
                   SUM(total_amount)
            FROM invoices
            WHERE (CAST(:fromDate AS date) IS NULL OR invoice_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate AS date) IS NULL OR invoice_date < CAST(:toDate AS date) + 1)
            GROUP BY TO_CHAR(invoice_date,'YYYY-MM-DD')
            ORDER BY TO_CHAR(invoice_date,'YYYY-MM-DD')
            """, nativeQuery = true)
    List<Object[]> getRevenueByDayFiltered(@Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT TO_CHAR(invoice_date,'YYYY-MM'),
                   SUM(total_amount)
            FROM invoices
            WHERE (CAST(:year AS integer) IS NULL OR EXTRACT(YEAR FROM invoice_date) = CAST(:year AS integer))
              AND (CAST(:month AS integer) IS NULL OR EXTRACT(MONTH FROM invoice_date) = CAST(:month AS integer))
            GROUP BY TO_CHAR(invoice_date,'YYYY-MM')
            ORDER BY TO_CHAR(invoice_date,'YYYY-MM')
            """, nativeQuery = true)
    List<Object[]> getRevenueByMonthFiltered(@Param("month") Integer month,
                                             @Param("year") Integer year);

    @Query(value = """
            SELECT COUNT(*)
            FROM invoices
            WHERE (CAST(:fromDate AS date) IS NULL OR invoice_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate AS date) IS NULL OR invoice_date < CAST(:toDate AS date) + 1)
            """, nativeQuery = true)
    long countFiltered(@Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate);
}