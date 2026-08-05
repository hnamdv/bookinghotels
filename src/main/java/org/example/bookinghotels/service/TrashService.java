package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Invoices;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.InvoiceRepository;
import org.example.bookinghotels.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrashService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // ==== Xem danh sách đã xóa ====

    public List<Invoices> getDeletedInvoices() {
        return invoiceRepository.findAllByDeleteAtTrue();
    }

    public List<User> getDeletedUsers() {
        return userRepository.findAllByDeleteAtTrue();
    }

    // ==== Khôi phục ====

    public void restoreInvoice(Integer id) {
        invoiceRepository.findById(id).ifPresent(invoice -> {
            invoice.setDeleteAt(false);
            invoiceRepository.save(invoice);
        });
    }

    public void restoreUser(Integer id) {
        userService.restoreUser(id);
    }
}