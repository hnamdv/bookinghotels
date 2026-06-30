package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.EmailService; // Đã thêm import EmailService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Autowired
    private EmailService emailService; // Đã bổ sung tiêm EmailService vào đây

    @Override
    @Transactional
    public Booking processBooking(
            Booking booking,
            BookingDetail detail,
            List<BookingFB> orderedFoods,
            String paymentMethod
    ) {
        // 1. Kiểm tra trùng lặp an toàn
        if (booking.getId() != null) {
            Optional<Invoices> existingInvoice = invoicesRepository.findFirstByBookingIdOrderByIdDesc(Long.valueOf(booking.getId()));
            if (existingInvoice.isPresent()) {
                return booking;
            }
        }

        // 2. Lưu Booking và đồng bộ thẳng xuống DB để chắc chắn ID đã tồn tại thực tế
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        // 3. Lấy RoomType kiểm tra tính hợp lệ
        if (detail.getRoom() == null || detail.getRoom().getRoomType() == null) {
            throw new RuntimeException("Thông tin phòng hoặc loại phòng không hợp lệ");
        }
        RoomType roomType = detail.getRoom().getRoomType();
        detail.setRoomType(roomType);

        // 4. Tính số đêm đặt phòng
        long days = ChronoUnit.DAYS.between(
                savedBooking.getCheckinDate(),
                savedBooking.getCheckoutDate()
        );
        if (days <= 0) {
            days = 1;
        }

        // 5. [QUAN TRỌNG] Phải gán thực thể gán "savedBooking" (đã có ID) cho detail
        detail.setBooking(savedBooking);

        // --- ĐỒNG BỘ TÍNH TOÁN GIÁ PHÒNG THEO MỨC GIẢM GIÁ 10% NHƯ TRANG PAYMENT ---
        double originalPrice = roomType.getPrice();
        double discountPercent = 10.0; // Mức giảm giá giống trang hiển thị
        double discountedPrice = originalPrice * (1 - (discountPercent / 100));

        detail.setPrice(discountedPrice * days);

        // Tiến hành lưu Detail sau khi liên kết chắc chắn đã có ID cha
        BookingDetail savedDetail = bookingDetailRepository.saveAndFlush(detail);

        // 6. Tính tổng tiền phòng (Bỏ phần tự động cộng roomType.getTaxAndFee() gây lệch tiền)
        double discount = detail.getDiscountAmount() == null ? 0 : detail.getDiscountAmount();
        double totalAmount = (discountedPrice * days) - discount;

        // 7. Thêm món ăn dịch vụ nếu có
        if (orderedFoods != null && !orderedFoods.isEmpty()) {
            for (BookingFB foodOrder : orderedFoods) {
                foodOrder.setBookingDetail(savedDetail);
                bookingFBRepository.save(foodOrder);
                totalAmount += (foodOrder.getPriceAtOrder() * foodOrder.getQuantity());
            }
        }

        // 8. Tạo mới và Lưu hóa đơn
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

    @Override
    public boolean isRoomAvailable(
            Integer roomId,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {
        return !bookingDetailRepository.existsOverlappingBooking(
                roomId,
                checkinDate,
                checkoutDate
        );
    }

    @Override
    public List<Integer> getAvailableRooms(
            List<Integer> allRoomIds,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {
        List<BookingDetail> overlappingBookings =
                bookingDetailRepository.findOverlappingBookings(
                        allRoomIds,
                        checkinDate,
                        checkoutDate
                );

        List<Integer> occupiedRoomIds = overlappingBookings.stream()
                .map(bd -> bd.getRoom().getId())
                .distinct()
                .toList();

        return allRoomIds.stream()
                .filter(roomId -> !occupiedRoomIds.contains(roomId))
                .toList();
    }

    @Override
    public void validateBooking(
            Integer roomId,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {
        if (checkinDate == null || checkoutDate == null) {
            throw new IllegalArgumentException(
                    "Ngày check-in và check-out không được để trống"
            );
        }

        if (!checkinDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException(
                    "Ngày check-in phải trước ngày check-out"
            );
        }

        if (checkinDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Không thể đặt phòng trong quá khứ"
            );
        }

        if (!isRoomAvailable(roomId, checkinDate, checkoutDate)) {
            throw new IllegalStateException(
                    "Phòng đã được đặt trong khoảng thời gian này"
            );
        }
    }

    @Override
    @Transactional
    public void updateStatusToPaid(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nội dung chuyển khoản trống."
            );
        }

        Pattern pattern = Pattern.compile("FEELHOMEBK(\\d+)");
        Matcher matcher = pattern.matcher(content.toUpperCase());

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "QR không hợp lệ."
            );
        }

        Long bookingId = Long.parseLong(matcher.group(1));

        Invoices invoice = invoicesRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy hóa đơn"
                        )
                );

        invoice.setPaymentStatus("PAID");
        invoicesRepository.save(invoice);

        try {
            Booking booking = invoice.getBooking();
            if (booking != null) {
                String customerEmail = booking.getEmail();
                String customerName = booking.getName();

                String roomName = "Phòng nghỉ FeelHome";
                if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
                    BookingDetail bd = booking.getBookingDetails().get(0);
                    if (bd.getRoomType() != null) {
                        roomName = bd.getRoomType().getNameType();
                    }
                }

                double amount = invoice.getTotalAmount();

                emailService.sendBookingConfirmation(customerEmail, customerName, roomName, amount);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi kích hoạt gửi email ngầm: " + e.getMessage());
        }
    }

    @Override
    public Booking getBookingById(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("FEELHOMEBK(\\d+)");
        Matcher matcher = pattern.matcher(content.toUpperCase());

        if (matcher.find()) {
            Integer bookingId = Integer.parseInt(matcher.group(1));
            return bookingRepository.findById(bookingId).orElse(null);
        }

        return null;
    }

    @Override
    public Invoices findInvoiceByBookingId(Long bookingId) {
        return invoicesRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn cho mã đặt phòng: " + bookingId));
    }

    // =====================================================
    // IMPLEMENTATION HÀM HỦY GIỮ PHÒNG KHI KHÁCH BẤM NÚT HỦY QR
    // =====================================================
    @Override
    @Transactional
    public void updateBookingStatus(Long bookingId, String status) {
        // 1. Tìm và cập nhật trạng thái thanh toán của Hóa đơn (Invoices) thành CANCELLED
        invoicesRepository.findFirstByBookingIdOrderByIdDesc(bookingId).ifPresent(invoice -> {
            invoice.setPaymentStatus(status);
            invoicesRepository.save(invoice);
        });

        // 2. Tìm Booking và cập nhật trạng thái của tất cả BookingDetail liên quan thành CANCELLED
        bookingRepository.findById(Math.toIntExact(bookingId)).ifPresent(booking -> {
            if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
                for (BookingDetail detail : booking.getBookingDetails()) {
                    detail.setStatus(status); // Chuyển trạng thái PENDING -> CANCELLED
                    bookingDetailRepository.save(detail);
                }
            }
        });
    }
}