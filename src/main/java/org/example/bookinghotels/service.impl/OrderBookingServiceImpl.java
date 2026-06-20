package org.example.bookinghotels.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.OrderBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderBookingServiceImpl implements OrderBookingService {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private FwbRepository fwbRepository;
    @Autowired
    private BookingFBRepository bookingFBRepository;
    @Autowired
    private InvoicesRepository invoicesRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Override
    @Transactional
    public Booking saveAndReturn(Booking booking, Integer roomTypeId) {
        // 1. Lưu Booking vào DB và lấy đối tượng vừa lưu (đã có ID)
        Booking savedBooking = bookingRepository.save(booking);

        // 2. Tạo BookingDetail cơ bản (bạn có thể bổ sung thêm logic gán RoomType tại đây nếu cần)
        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);

        // Giả sử lấy thông tin roomType từ ID (bạn cần truyền đúng tham số này vào)
        RoomType rt = roomTypeRepository.findById(roomTypeId).orElse(null);
        detail.setRoomType(rt);
        detail.setPrice(rt != null ? rt.getPrice() : 0.0);

        bookingDetailRepository.save(detail);

        // 3. Trả về Booking đã có ID để Controller dùng
        return savedBooking;
    }
    @Override
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu: Nếu một bảng lỗi, tất cả sẽ rollback
    public Booking processBooking(Booking booking, BookingDetail detail, List<BookingFB> orderedFoods, String paymentMethod) {
        // 1. Lưu thông tin Booking chính
        Booking savedBooking = bookingRepository.save(booking);

        // 2. Lấy thông tin giá phòng từ DB để tính toán bảo mật
        RoomType roomType = roomTypeRepository.findById(detail.getRoomType().getId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không hợp lệ"));

        // 3. Cấu hình và lưu chi tiết đặt phòng
        detail.setBooking(savedBooking);
        detail.setPrice(roomType.getPrice());
        BookingDetail savedDetail = bookingDetailRepository.save(detail);

        // 4. Tính toán tổng tiền phòng
        double totalAmount = (roomType.getPrice() * detail.getRoomQuantity()) + roomType.getTaxAndFee() - detail.getDiscountAmount();

        // 5. Nếu khách có gọi thêm đồ ăn/thức uống (F&B) thì lưu vào bảng trung gian và cộng dồn tiền
        if (orderedFoods != null && !orderedFoods.isEmpty()) {
            for (BookingFB foodOrder : orderedFoods) {
                foodOrder.setBookingDetail(savedDetail);
                bookingFBRepository.save(foodOrder);

                // Cộng dồn tiền dịch vụ ăn uống vào hóa đơn
                totalAmount += (foodOrder.getPriceAtOrder() * foodOrder.getQuantity());
            }
        }

        // 6. Tự động sinh Hóa đơn (Invoice)
        Invoices invoice = new Invoices();
        invoice.setBooking(savedBooking);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setPaymentStatus("PENDING");
        invoice.setUser(detail.getUser());
        invoicesRepository.save(invoice);

        return savedBooking;
    }

    @Override
    public List<Invoices> getAllInvoices() {
        return invoicesRepository.findAll();
    }

    @Override
    public List<FwB> getAllAvailableFoods() {
        return fwbRepository.findAll();
    }

    //Bao//
    @Override
    public boolean isRoomAvailable(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate) {
        return !bookingDetailRepository.existsOverlappingBooking(roomId, checkinDate, checkoutDate);
    }

    @Override
    public List<Integer> getAvailableRooms(List<Integer> allRoomIds, LocalDate checkinDate, LocalDate checkoutDate) {
        // B1: Lấy danh sách booking bị gối lịch
        List<BookingDetail> overlappingBookings =
                bookingDetailRepository.findOverlappingBookings(allRoomIds, checkinDate, checkoutDate);

        // B2: Lấy ra các roomId đã bị đặt (gối lịch)
        List<Integer> occupiedRoomIds = overlappingBookings.stream()
                .map(bd -> bd.getRoom().getId())
                .distinct()
                .toList();

        // B3: LOẠI TRỪ phòng gối lịch, trả về phòng trống
        return allRoomIds.stream()
                .filter(roomId -> !occupiedRoomIds.contains(roomId))
                .toList();
    }

    @Override
    public void validateBooking(Integer roomId, LocalDate checkinDate, LocalDate checkoutDate) {
        if (checkinDate == null || checkoutDate == null) {
            throw new IllegalArgumentException("Ngày check-in và check-out không được để trống");
        }
        if (!checkinDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Ngày check-in phải trước ngày check-out");
        }
        if (checkinDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể đặt phòng trong quá khứ");
        }
        if (!isRoomAvailable(roomId, checkinDate, checkoutDate)) {
            throw new IllegalStateException("Phòng đã được đặt trong khoảng thời gian này (gối lịch)");
        }
    }
    //
    //
    //
    //
    //
    //                 Trang cua khach hang ////

    //

    @Autowired
    private JavaMailSender mailSender;

    public void saveAndSendEmail(Booking booking, Integer roomId) {
        // 1. Lưu Booking
        bookingRepository.save(booking);
// 2. Tạm thời ẩn gửi mail để không bị lỗi
    /*
    sendEmail(booking);

        // 2. Gửi mail (Sử dụng MimeMessage để gửi HTML cho đẹp)
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(booking.getEmail());
            helper.setSubject("Xác nhận đơn đặt phòng thành công");
            helper.setText("<h1>Cảm ơn bạn đã đặt phòng!</h1><p>Thông tin check-in: " + booking.getCheckinDate() + "</p>", true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    */
    }


    @Override
    public List<RoomType> getAvailableRoomTypes(LocalDate checkin, LocalDate checkout) {
        List<RoomType> allRoomTypes = roomTypeRepository.findAll();
        List<RoomType> availableTypes = new ArrayList<>();

        for (RoomType type : allRoomTypes) {
            // 1. Đếm số lượng phòng loại này đã bị đặt trong khoảng thời gian checkin - checkout
            // Điều kiện trùng lịch: (startA <= endB) AND (endA >= startB)
            long bookedCount = bookingRepository.countBookedRoomsByTypeId(
                    type.getId(), checkin, checkout);

            // 2. Nếu số phòng đã đặt < tổng số phòng thì loại này còn trống
            if (bookedCount < type.getTotalRooms()) {
                availableTypes.add(type);
            }
        }
        return availableTypes;
    }
}