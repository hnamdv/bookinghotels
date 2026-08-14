package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoices, Integer> {

    @Query("SELECT i FROM Invoices i LEFT JOIN i.user u WHERE " +
            "i.deleteAt = false " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "str(i.id) LIKE CONCAT('%', :keyword, '%') OR " +
            "u.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR :status = '' OR i.paymentStatus = :status)")
    List<Invoices> searchInvoices(@Param("keyword") String keyword,
                                  @Param("status") String status);

    Optional<Invoices> findByBookingId(Integer bookingId);

    List<Invoices> findAllByDeleteAtFalse();

    List<Invoices> findAllByDeleteAtTrue();
}