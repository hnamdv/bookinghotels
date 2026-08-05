package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<Invoices, Integer> {

    @Query("SELECT i FROM Invoices i WHERE i.booking.id = :bookingId ORDER BY i.id DESC")
    List<Invoices> findInvoicesByBookingIdDesc(@Param("bookingId") Integer bookingId);

    default Optional<Invoices> findByBookingId(Integer bookingId) {
        if (bookingId == null) return Optional.empty();
        List<Invoices> invoices = findInvoicesByBookingIdDesc(bookingId);
        return invoices.isEmpty() ? Optional.empty() : Optional.of(invoices.get(0));
    }

    default Optional<Invoices> findByBookingId(Long bookingId) {
        if (bookingId == null) return Optional.empty();
        return findByBookingId(bookingId.intValue());
    }

    default Optional<Invoices> findFirstByBookingIdOrderByIdDesc(Long bookingId) {
        return findByBookingId(bookingId);
    }

    default Optional<Invoices> findFirstByBookingIdOrderByIdDesc(Integer bookingId) {
        return findByBookingId(bookingId);
    }

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
