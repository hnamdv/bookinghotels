package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Invoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoices, Integer> { // Đổi Long thành Integer cho khớp với Entity id nhé

    @Query("SELECT i FROM Invoices i WHERE " +
            "(:keyword IS NULL OR trim(:keyword) = '' OR " +
            " str(i.id) LIKE :keyword_1 OR " +
            " i.user.name LIKE :keyword_1) " +
            "AND (:status IS NULL OR trim(:status) = '' OR i.paymentStatus = :status)")
    List<Invoices> searchInvoices(@Param("keyword") String keyword,
                                  @Param("keyword_1") String keyword_1,
                                  @Param("status") String status);
}