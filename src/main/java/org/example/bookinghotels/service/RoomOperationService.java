package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.ActivityLogRepository;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class RoomOperationService {
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailService emailService;

    public RoomOperationService(RoomRepository roomRepository,
                                BookingDetailRepository bookingDetailRepository,
                                ActivityLogRepository activityLogRepository,
                                EmailService emailService) {
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.activityLogRepository = activityLogRepository;
        this.emailService = emailService;
    }

    public List<RoomView> buildRoomViews() {
        return buildRoomViews(null);
    }

    public List<RoomView> buildRoomViews(Integer roomTypeId) {
        LocalDate today = LocalDate.now();
        List<Room> rooms = roomTypeId == null ? roomRepository.findAll() : roomRepository.findByRoomTypeId(roomTypeId);
        rooms.sort(Comparator.comparing(Room::getRoomNumber, Comparator.nullsLast(String::compareToIgnoreCase)));
        List<Integer> roomIds = rooms.stream().map(Room::getId).filter(Objects::nonNull).toList();
        Map<Integer, List<BookingDetail>> bookingsByRoom = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (BookingDetail detail : bookingDetailRepository.findActiveBookingsByRoomIds(roomIds)) {
                if (detail.getRoom() != null && detail.getRoom().getId() != null) {
                    bookingsByRoom.computeIfAbsent(detail.getRoom().getId(), key -> new ArrayList<>()).add(detail);
                }
            }
        }
        List<RoomView> result = new ArrayList<>();
        for (Room room : rooms) {
            List<BookingDetail> bookings = bookingsByRoom.getOrDefault(room.getId(), List.of());
            BookingDetail current = bookings.stream()
                    .filter(this::isActiveForRoom)
                    .filter(b -> !b.getBooking().getCheckinDate().isAfter(today)
                            && !b.getBooking().getCheckoutDate().isBefore(today))
                    .sorted(Comparator.comparing(b -> b.getBooking().getCheckinDate()))
                    .findFirst().orElse(null);
            BookingDetail next = bookings.stream()
                    .filter(this::isActiveForRoom)
                    .filter(b -> b.getBooking().getCheckinDate().isAfter(today))
                    .sorted(Comparator.comparing(b -> b.getBooking().getCheckinDate()))
                    .findFirst().orElse(null);
            String state = deriveState(current, next, today);
            LocalDateTime checkinAt = current == null ? null : latestActionTime("CHECK_IN", current.getId());
            LocalDateTime checkoutAt = current == null ? null : latestCheckoutTime(current.getId());
            result.add(new RoomView(room, current, next, state, checkinAt, checkoutAt));
        }
        return result;
    }

    public String deriveState(BookingDetail current, BookingDetail next, LocalDate today) {
        if (current != null) {
            String status = normalize(current.getStatus());
            if ("CHECKED_IN".equals(status)) {
                if (current.getBooking().getCheckoutDate().isBefore(today)) return "CHECKOUT_OVERDUE";
                if (current.getBooking().getCheckoutDate().isEqual(today)) return "CHECKOUT_TODAY";
                return "OCCUPIED";
            }
            if ("PENDING".equals(status)) return "PENDING_BOOKING";
            if (Set.of("CONFIRMED", "APPROVED").contains(status)) {
                if (current.getBooking().getCheckinDate().isBefore(today)) return "CHECKIN_OVERDUE";
                if (current.getBooking().getCheckinDate().isEqual(today)) return "ARRIVAL_TODAY";
                return "RESERVED";
            }
        }
        if (next != null) return "AVAILABLE_NEXT_RESERVED";
        return "AVAILABLE";
    }

    @Transactional
    public BookingDetail checkIn(Integer detailId) {
        BookingDetail detail = getDetail(detailId);
        String status = normalize(detail.getStatus());
        if (!Set.of("CONFIRMED", "APPROVED").contains(status)) {
            throw new IllegalStateException("Đơn phải ở trạng thái đã duyệt mới được check-in.");
        }
        if (detail.getRoom() == null) throw new IllegalStateException("Đơn chưa được xếp phòng.");
        boolean occupied = bookingDetailRepository.findByRoomIdWithBooking(detail.getRoom().getId()).stream()
                .anyMatch(other -> !Objects.equals(other.getId(), detail.getId())
                        && "CHECKED_IN".equals(normalize(other.getStatus())));
        if (occupied) throw new IllegalStateException("Phòng vẫn còn khách chưa checkout.");
        detail.setStatus("CHECKED_IN");
        BookingDetail saved = bookingDetailRepository.save(detail);
        log("CHECK_IN", saved, "Check-in thủ công");
        notifyCustomer(saved, "CHECK_IN", "Bạn đã check-in thành công", "Phòng " + saved.getRoom().getRoomNumber() + " đã được bàn giao.");
        return saved;
    }

    @Transactional
    public BookingDetail checkOut(Integer detailId, boolean automatic) {
        BookingDetail detail = getDetail(detailId);
        if (!"CHECKED_IN".equals(normalize(detail.getStatus()))) {
            throw new IllegalStateException("Chỉ đơn đang lưu trú mới được checkout.");
        }
        detail.setStatus("CHECKED_OUT");
        BookingDetail saved = bookingDetailRepository.save(detail);
        log(automatic ? "AUTO_CHECK_OUT" : "CHECK_OUT", saved,
                automatic ? "Checkout tự động theo ngày trả phòng" : "Checkout thủ công");
        notifyCustomer(saved, "CHECK_OUT", "Xác nhận checkout",
                "FeelHome đã ghi nhận hoàn tất trả phòng " + saved.getRoom().getRoomNumber() + ".");
        return saved;
    }

    @Transactional
    public BookingDetail markNoShow(Integer detailId) {
        BookingDetail detail = getDetail(detailId);
        if (!Set.of("CONFIRMED", "APPROVED").contains(normalize(detail.getStatus()))) {
            throw new IllegalStateException("Chỉ đơn đã duyệt chưa check-in mới có thể đánh dấu no-show.");
        }
        detail.setStatus("NO_SHOW");
        BookingDetail saved = bookingDetailRepository.save(detail);
        log("NO_SHOW", saved, "Khách không đến nhận phòng");
        notifyCustomer(saved, "NO_SHOW", "Đơn đặt phòng đã được ghi nhận không đến",
                "Vui lòng liên hệ FeelHome nếu cần hỗ trợ.");
        return saved;
    }

    @Transactional
    public int autoCheckoutDueBookings() {
        LocalDate today = LocalDate.now();
        if (LocalTime.now().isBefore(LocalTime.of(13, 0))) return 0;
        int count = 0;
        for (BookingDetail detail : bookingDetailRepository.findOperationalBookings()) {
            if ("CHECKED_IN".equals(normalize(detail.getStatus()))
                    && !detail.getBooking().getCheckoutDate().isAfter(today)) {
                String marker = "DETAIL#" + detail.getId() + "|DATE#" + today;
                if (!activityLogRepository.existsByActionAndTableNameAndDescriptionContaining("AUTO_CHECK_OUT", "booking_detail", marker)) {
                    checkOut(detail.getId(), true);
                    count++;
                }
            }
        }
        return count;
    }

    @Transactional
    public void sendLateCheckInReminders() {
        LocalDate today = LocalDate.now();
        if (LocalTime.now().isBefore(LocalTime.of(14, 30))) return;
        for (BookingDetail detail : bookingDetailRepository.findOperationalBookings()) {
            if (Set.of("CONFIRMED", "APPROVED").contains(normalize(detail.getStatus()))
                    && !detail.getBooking().getCheckinDate().isAfter(today)) {
                String marker = "LATE_CHECKIN|DETAIL#" + detail.getId() + "|DATE#" + today;
                if (!activityLogRepository.existsByActionAndTableNameAndDescriptionContaining("REMINDER", "booking_detail", marker)) {
                    notifyCustomer(detail, "REMINDER", "Nhắc nhận phòng FeelHome",
                            "Đơn của bạn chưa được check-in. Vui lòng liên hệ lễ tân nếu bạn đến trễ.");
                    ActivityLog log = new ActivityLog();
                    log.setAction("REMINDER");
                    log.setTableName("booking_detail");
                    log.setDescription(marker);
                    log.setCreatedAt(LocalDateTime.now());
                    activityLogRepository.save(log);
                }
            }
        }
    }

    @Transactional
    public BookingDetail updateStatus(Integer detailId, String requestedStatus) {
        BookingDetail detail = getDetail(detailId);
        String status = normalize(requestedStatus);
        Set<String> allowed = Set.of("PENDING", "APPROVED", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "NO_SHOW", "CANCELLED");
        if (!allowed.contains(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        String old = normalize(detail.getStatus());
        if ("CHECKED_IN".equals(status)) return checkIn(detailId);
        if ("CHECKED_OUT".equals(status)) {
            if (!"CHECKED_IN".equals(old)) throw new IllegalStateException("Phải check-in trước khi checkout.");
            return checkOut(detailId, false);
        }
        detail.setStatus(status);
        BookingDetail saved = bookingDetailRepository.save(detail);
        log("STATUS_UPDATE", saved, "Admin đổi trạng thái " + old + " -> " + status);
        if ("CANCELLED".equals(status) || "NO_SHOW".equals(status)) {
            notifyCustomer(saved, status, "Cập nhật trạng thái đặt phòng", "Đơn đặt phòng của bạn đã chuyển sang trạng thái " + status + ".");
        }
        return saved;
    }

    @Transactional
    public void sendCheckoutDueReminders() {
        LocalDate today = LocalDate.now();
        if (LocalTime.now().isBefore(LocalTime.NOON)) return;
        for (BookingDetail detail : bookingDetailRepository.findOperationalBookings()) {
            if ("CHECKED_IN".equals(normalize(detail.getStatus()))
                    && !detail.getBooking().getCheckoutDate().isAfter(today)) {
                String marker = "CHECKOUT_DUE|DETAIL#" + detail.getId() + "|DATE#" + today;
                if (!activityLogRepository.existsByActionAndTableNameAndDescriptionContaining("REMINDER", "booking_detail", marker)) {
                    notifyCustomer(detail, "REMINDER", "Đã đến giờ trả phòng",
                            "Đã đến giờ checkout. Vui lòng hoàn tất thủ tục trả phòng tại quầy lễ tân.");
                    ActivityLog log = new ActivityLog();
                    log.setAction("REMINDER");
                    log.setTableName("booking_detail");
                    log.setDescription(marker);
                    log.setCreatedAt(LocalDateTime.now());
                    activityLogRepository.save(log);
                }
            }
        }
    }

    private LocalDateTime latestActionTime(String action, Integer detailId) {
        return activityLogRepository
                .findFirstByActionAndTableNameAndDescriptionContainingOrderByCreatedAtDesc(
                        action, "booking_detail", "DETAIL#" + detailId)
                .map(ActivityLog::getCreatedAt).orElse(null);
    }

    private LocalDateTime latestCheckoutTime(Integer detailId) {
        LocalDateTime manual = latestActionTime("CHECK_OUT", detailId);
        LocalDateTime automatic = latestActionTime("AUTO_CHECK_OUT", detailId);
        if (manual == null) return automatic;
        if (automatic == null) return manual;
        return manual.isAfter(automatic) ? manual : automatic;
    }

    public BookingDetail getDetail(Integer id) {
        return bookingDetailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking detail #" + id));
    }

    private boolean isActiveForRoom(BookingDetail detail) {
        return Set.of("PENDING", "APPROVED", "CONFIRMED", "CHECKED_IN").contains(normalize(detail.getStatus()));
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private void log(String action, BookingDetail detail, String text) {
        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setTableName("booking_detail");
        log.setDescription(text + " | DETAIL#" + detail.getId() + " | ROOM#" +
                (detail.getRoom() == null ? "-" : detail.getRoom().getRoomNumber()));
        log.setCreatedAt(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    private void notifyCustomer(BookingDetail detail, String type, String subject, String body) {
        if (detail.getBooking() != null && detail.getBooking().getEmail() != null
                && !detail.getBooking().getEmail().isBlank()) {
            emailService.sendStayNotification(detail.getBooking().getEmail(),
                    detail.getBooking().getName(), subject, body,
                    detail.getRoom() == null ? "Chưa xếp phòng" : detail.getRoom().getRoomNumber(),
                    detail.getBooking().getCheckinDate(), detail.getBooking().getCheckoutDate());
        }
    }

    public record RoomView(Room room, BookingDetail currentBooking, BookingDetail nextBooking, String state, LocalDateTime checkinAt, LocalDateTime checkoutAt) {}
}
