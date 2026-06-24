package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.OrderBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Override
    @Transactional
    public Booking processBooking(
            Booking booking,
            BookingDetail detail,
            List<BookingFB> orderedFoods,
            String paymentMethod
    ) {

        // 1. Lưu booking trước + flush ngay xuống DB để sinh ra ID (Ví dụ: ID = 50)
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        // 2. Lấy trực tiếp RoomType từ Room đã được tìm thấy ở Controller để tránh lỗi đồng bộ JPA
        if (detail.getRoom() == null || detail.getRoom().getRoomType() == null) {
            throw new RuntimeException("Thông tin phòng hoặc loại phòng không hợp lệ");
        }
        RoomType roomType = detail.getRoom().getRoomType();

        // Gán ngược lại thực thể roomType chuẩn vào detail
        detail.setRoomType(roomType);

        // 3. Tính số đêm đặt phòng
        long days = ChronoUnit.DAYS.between(
                savedBooking.getCheckinDate(),
                savedBooking.getCheckoutDate()
        );

        if (days <= 0) {
            days = 1;
        }

        // 4. Save booking detail liên kết với Booking vừa tạo
        detail.setBooking(savedBooking);
        detail.setPrice(roomType.getPrice() * days);
        BookingDetail savedDetail = bookingDetailRepository.save(detail);

        // 5. Tính tổng tiền phòng (đã gồm thuế phí và giảm giá)
        double discount = detail.getDiscountAmount() == null
                ? 0
                : detail.getDiscountAmount();

        double totalAmount =
                (roomType.getPrice() * days)
                        + roomType.getTaxAndFee()
                        - discount;

        // 6. Nếu có dịch vụ đồ ăn / thức uống kèm theo
        if (orderedFoods != null && !orderedFoods.isEmpty()) {
            for (BookingFB foodOrder : orderedFoods) {
                foodOrder.setBookingDetail(savedDetail);
                bookingFBRepository.save(foodOrder);

                totalAmount += (
                        foodOrder.getPriceAtOrder()
                                * foodOrder.getQuantity()
                );
            }
        }

        // 7. Tạo mới và Lưu hóa đơn (Invoice) đảm bảo khóa ngoại booking_id đã tồn tại
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

        Invoices invoice = invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy hóa đơn"
                        )
                );

        invoice.setPaymentStatus("PAID");
        invoicesRepository.save(invoice);
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

    // --- FIX LỖI TRẢ VỀ NULL TẠI ĐÂY ---
    @Override
    public Invoices findInvoiceByBookingId(Long bookingId) {
        return invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn cho mã đặt phòng: " + bookingId));
    }
}