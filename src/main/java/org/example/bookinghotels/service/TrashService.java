package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrashService {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // ==== Xem danh sách hóa đơn đã xóa mềm ====
    public List<BookingDetail> getDeletedInvoices() {
        return bookingDetailRepository.findAllByDeleteAtTrue();
    }

    // ==== Xem danh sách nhân viên đã xóa mềm ====
    public List<User> getDeletedUsers() {
        return userRepository.findAllByDeleteAtTrue();
    }

    // ==== Khôi phục hóa đơn ====
    public void restoreInvoice(Integer id) {
        bookingDetailRepository.findById(id).ifPresent(detail -> {
            detail.setDeleteAt(false); // Chuyển lại trạng thái chưa xóa
            bookingDetailRepository.save(detail);
        });
    }

    // ==== Khôi phục nhân viên ====
    public void restoreUser(Integer id) {
        userService.restoreUser(id);
    }
}