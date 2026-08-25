package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.*;
import org.example.bookinghotels.repository.*;
import org.example.bookinghotels.service.OrderBookingService;
import org.example.bookinghotels.service.PromotionPricingService;
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

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PromotionPricingService promotionPricingService;

    @Override
    @Transactional
    public Booking processBooking(
            Booking booking,
            BookingDetail detail,
            List<BookingFB> orderedFoods,
            String paymentMethod
    ) {

        // 1. Lưu booking trước + flush ngay xuống DB để sinh ra ID
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

        // 4. Tính lại ưu đãi tại backend để không phụ thuộc dữ liệu từ trình duyệt.
        PromotionPricingService.PriceQuote quote = promotionPricingService.quote(
                roomType,
                savedBooking.getCheckinDate(),
                savedBooking.getCheckoutDate()
        );
        double originalRoomTotal = quote.originalNightlyPrice() * days;
        double discountedRoomTotal = quote.effectiveNightlyPrice() * days;
        double discount = Math.max(0D, originalRoomTotal - discountedRoomTotal);

        // 5. Lưu booking detail với giá thực trả sau ưu đãi.
        detail.setBooking(savedBooking);
        detail.setPrice(discountedRoomTotal);
        detail.setDiscountAmount(discount);
        BookingDetail savedDetail = bookingDetailRepository.saveAndFlush(detail);

        // 6. Tổng hóa đơn = giá phòng sau ưu đãi (Không cộng dồn taxAndFee để khớp tuyệt đối với giao diện)
        double totalAmount = discountedRoomTotal;

        // 7. Nếu có dịch vụ đồ ăn / thức uống kèm theo
        if (orderedFoods != null && !orderedFoods.isEmpty()) {
            for (BookingFB foodOrder : orderedFoods) {
                foodOrder.setBookingDetail(savedDetail);

                // Đảm bảo không bị lỗi null priceAtOrder
                if (foodOrder.getPriceAtOrder() == null && foodOrder.getFwb() != null) {
                    foodOrder.setPriceAtOrder(foodOrder.getFwb().getPrice());
                }

                bookingFBRepository.save(foodOrder);

                totalAmount += (
                        foodOrder.getPriceAtOrder()
                                * foodOrder.getQuantity()
                );
            }
        }

        // 8. Tạo mới và Lưu hóa đơn (Invoice) đảm bảo khóa ngoại booking_id đã tồn tại
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
    @Transactional
    public Booking processBookingAutoAssign(
            Booking booking,
            BookingDetail detail,
            Integer roomTypeId,
            List<BookingFB> orderedFoods,
            String paymentMethod
    ) {
        if (roomTypeId == null) {
            throw new IllegalArgumentException("Thiếu loại phòng cần đặt");
        }
        if (booking.getCheckinDate() == null || booking.getCheckoutDate() == null) {
            throw new IllegalArgumentException("Ngày nhận và ngày trả không được để trống");
        }
        if (!booking.getCheckoutDate().isAfter(booking.getCheckinDate())) {
            booking.setCheckoutDate(booking.getCheckinDate().plusDays(1));
        }
        if (booking.getCheckinDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể đặt phòng trong quá khứ");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Loại phòng không tồn tại"));

        Room assignedRoom = findFirstAvailableRoomByType(
                roomTypeId,
                booking.getCheckinDate(),
                booking.getCheckoutDate()
        );
        if (assignedRoom == null) {
            throw new IllegalStateException("Loại phòng này đã hết phòng trong khoảng ngày đã chọn");
        }
        detail.setRoom(assignedRoom);
        detail.setRoomType(roomType);
        detail.setRoomQuantity(1);
        if (detail.getStatus() == null || detail.getStatus().isBlank()) {
            detail.setStatus("PENDING");
        }
        return processBooking(booking, detail, orderedFoods, paymentMethod);
    }

    @Override
    public List<Invoices> getAllInvoices() {
        return invoicesRepository.findAll();
    }

    @Override
    public List<FwB> getAllAvailableFoods() {
        // Chỉ hiển thị các dịch vụ/phụ thu có tính phí ở trang thanh toán.
        // Tiện ích miễn phí dùng để tick vào loại phòng, không hiện ở payment để tránh lệch với admin.
        return fwbRepository.findAll().stream()
                .filter(f -> f != null)
                .filter(f -> f.getStatus() == null
                        || f.getStatus().isBlank()
                        || "ACTIVE".equalsIgnoreCase(f.getStatus())
                        || "SHOW".equalsIgnoreCase(f.getStatus()))
                .filter(f -> f.getPrice() > 0D)
                .toList();
    }

    @Override
    public boolean isRoomAvailable(
            Integer roomId,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {
        LocalDate[] range = normalizeDateRange(checkinDate, checkoutDate);
        return !bookingDetailRepository.existsOverlappingBooking(
                roomId,
                range[0],
                range[1]
        );
    }

    @Override
    public List<Integer> getAvailableRooms(
            List<Integer> allRoomIds,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {

        if (allRoomIds == null || allRoomIds.isEmpty()) {
            return List.of();
        }
        LocalDate[] range = normalizeDateRange(checkinDate, checkoutDate);

        List<BookingDetail> overlappingBookings =
                bookingDetailRepository.findOverlappingBookings(
                        allRoomIds,
                        range[0],
                        range[1]
                );

        List<Integer> occupiedRoomIds = overlappingBookings.stream()
                .filter(bd -> bd.getRoom() != null && bd.getRoom().getId() != null)
                .map(bd -> bd.getRoom().getId())
                .distinct()
                .toList();

        return allRoomIds.stream()
                .filter(roomId -> !occupiedRoomIds.contains(roomId))
                .toList();
    }

    @Override
    public Room findFirstAvailableRoomByType(Integer roomTypeId, LocalDate checkinDate, LocalDate checkoutDate) {
        if (roomTypeId == null) return null;
        LocalDate[] range = normalizeDateRange(checkinDate, checkoutDate);
        // Sử dụng repository đã check status = APPROVED
        List<Room> availableRooms = roomRepository.findAvailableRooms(roomTypeId, range[0], range[1]);
        return availableRooms.isEmpty() ? null : availableRooms.get(0);
    }

    @Override
    public long countAvailableRoomsByType(Integer roomTypeId, LocalDate checkinDate, LocalDate checkoutDate) {
        if (roomTypeId == null) return 0L;
        LocalDate[] range = normalizeDateRange(checkinDate, checkoutDate);
        // Sử dụng repository đã check status = APPROVED
        return roomRepository.findAvailableRooms(roomTypeId, range[0], range[1]).size();
    }

    private LocalDate[] normalizeDateRange(LocalDate checkinDate, LocalDate checkoutDate) {
        LocalDate start = checkinDate == null ? LocalDate.now() : checkinDate;
        LocalDate end = checkoutDate == null ? start.plusDays(1) : checkoutDate;
        if (!end.isAfter(start)) end = start.plusDays(1);
        return new LocalDate[]{start, end};
    }

    @Override
    public void validateBooking(
            Integer roomId,
            LocalDate checkinDate,
            LocalDate checkoutDate
    ) {

        LocalDate[] range = normalizeDateRange(checkinDate, checkoutDate);
        checkinDate = range[0];
        checkoutDate = range[1];

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

    @Override
    public Invoices findInvoiceByBookingId(Long bookingId) {
        return invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn cho mã đặt phòng: " + bookingId));
    }

    @Override
    @Transactional
    public void updateBookingStatus(Long bookingId, String status) {
        List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId.intValue());
        for (BookingDetail detail : details) {
            detail.setStatus(status);
            bookingDetailRepository.save(detail);
        }

        Invoices invoice = findInvoiceByBookingId(bookingId);
        if (invoice != null) {
            invoice.setPaymentStatus("CANCELLED".equals(status) ? "CANCELLED" : status);
        }
    }
}