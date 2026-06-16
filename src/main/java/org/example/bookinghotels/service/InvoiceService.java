package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<Invoices> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoices> searchInvoices(String keyword, String status) {
        // Tự động tạo chứa dấu % để tìm kiếm theo kiểu LIKE
        String keywordWithWildcard = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword + "%" : null;

        // Truyền đủ 3 tham số xuống Repository: keyword, keyword_1 (đã thêm %), status
        return invoiceRepository.searchInvoices(keyword, keywordWithWildcard, status);
    }

    // Đổi hẳn sang Integer để đồng bộ với thuộc tính id trong entity Invoices
    public Invoices findById(Integer id) {
        Optional<Invoices> invoice = invoiceRepository.findById(id);
        return invoice.orElse(null);
    }
}