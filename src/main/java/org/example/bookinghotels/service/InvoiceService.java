package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<Invoices> searchInvoices(String keyword, String status) {
        return invoiceRepository.searchInvoices(keyword, status);
    }

    public Invoices findById(Integer id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    public void softDeleteInvoice(Integer id) {
        Invoices invoice = findById(id);
        if (invoice != null) {
            invoice.setDeleteAt(true);
            invoiceRepository.save(invoice);
        }
    }

    public Invoices findByBookingId(Integer bookingId) {
        return invoiceRepository.findByBookingId(bookingId).orElse(null);
    }
}