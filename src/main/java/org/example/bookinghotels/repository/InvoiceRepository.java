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

    @Query("SELECT i FROM Invoices i WHERE " +
            "i.deleteAt = false " +
            "AND (:keyword IS NULL OR trim(:keyword) = '' OR " +
            " str(i.id) LIKE :keyword_1 OR " +
            " i.user.name LIKE :keyword_1) " +
            "AND (:status IS NULL OR trim(:status) = '' OR i.paymentStatus = :status)")
    List<Invoices> searchInvoices(@Param("keyword") String keyword,
                                  @Param("keyword_1") String keyword_1,
                                  @Param("status") String status);

    Optional<Invoices> findByBookingId(Integer bookingId);

    // Danh sách hóa đơn chưa xóa (dùng cho trang chính)
    List<Invoices> findAllByDeleteAtFalse();

    // ==== Dùng cho trang trash ====
    List<Invoices> findAllByDeleteAtTrue();
}