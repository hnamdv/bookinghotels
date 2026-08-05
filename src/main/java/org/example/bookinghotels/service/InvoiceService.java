package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    // Lấy tất cả hóa đơn (chưa xóa)
    public List<Invoices> getAllInvoices() {
        return invoiceRepository.findAllByDeleteAtFalse();
    }

    // Tìm kiếm hóa đơn
    public List<Invoices> searchInvoices(String keyword, String status) {
        String keywordWithWildcard =
                (keyword != null && !keyword.trim().isEmpty())
                        ? "%" + keyword + "%"
                        : null;

        return invoiceRepository.searchInvoices(
                keyword,
                keywordWithWildcard,
                status
        );
    }

    // Tìm theo ID hóa đơn
    public Invoices findById(Integer id) {
        Optional<Invoices> invoice = invoiceRepository.findById(id);
        return invoice.orElse(null);
    }

    // Tìm hóa đơn theo Booking ID
    public Invoices findByBookingId(Integer bookingId) {
        Optional<Invoices> invoice = invoiceRepository.findByBookingId(bookingId);
        return invoice.orElse(null);
    }

    // ==== Xóa mềm ====
    @Transactional
    public void softDeleteInvoice(Integer id) {
        invoiceRepository.findById(id).ifPresent(invoice -> {
            invoice.setDeleteAt(true);
            invoiceRepository.save(invoice);
        });
    }
}