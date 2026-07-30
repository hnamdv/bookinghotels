package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<Invoices, Long> {

    // --- SỬA TẠI ĐÂY: Thay thế findByBookingId cũ để lấy hóa đơn mới nhất, tránh lỗi trả về 2 kết quả ---
    Optional<Invoices> findFirstByBookingIdOrderByIdDesc(Long bookingId);

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
}