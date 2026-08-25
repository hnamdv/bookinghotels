package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.BookingFB;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.example.bookinghotels.repository.BookingFBRepository;
import org.example.bookinghotels.repository.BookingRepository;
import org.example.bookinghotels.repository.FwbRepository;
import org.example.bookinghotels.service.FwBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/pos")
@PreAuthorize("hasAuthority('ROLE_FWB')")
public class PosController {

    @Autowired
    private FwBService fwBService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private BookingFBRepository bookingFBRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== API LẤY DANH SÁCH MÓN CHO POS =====
    @GetMapping("/menu")
    @ResponseBody
    public List<Map<String, Object>> getMenu() {
        return fwBService.getAll();
    }

    // ===== API LẤY DANH SÁCH BOOKING CÓ THỂ GỌI DỊCH VỤ PHÒNG =====
    @GetMapping("/bookings")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllBookings() {
        List<BookingDetail> details = bookingDetailRepository.findAllWithDetails();
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Integer> usedBookingIds = new LinkedHashSet<>();

        for (BookingDetail detail : details) {
            if (detail == null || detail.getBooking() == null || detail.getBooking().getId() == null) continue;
            String status = detail.getStatus() == null ? "" : detail.getStatus().toUpperCase(Locale.ROOT);
            if (Set.of("CHECKED_OUT", "CANCELLED", "REJECTED", "NO_SHOW").contains(status)) continue;
            if (!usedBookingIds.add(detail.getBooking().getId())) continue;

            Booking b = detail.getBooking();
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("bookingDetailId", detail.getId());
            item.put("name", b.getName() != null ? b.getName() : "");
            item.put("phone", b.getPhone() != null ? b.getPhone() : "");
            item.put("checkinDate", b.getCheckinDate());
            item.put("checkoutDate", b.getCheckoutDate());
            item.put("status", detail.getStatus());
            item.put("roomNumber", detail.getRoom() != null ? detail.getRoom().getRoomNumber() : "Chưa xếp phòng");
            item.put("roomType", detail.getRoomType() != null ? detail.getRoomType().getNameType() : "");
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    // ===== API TẠO ORDER =====
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createFnbOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer bookingId = (Integer) payload.get("bookingId");
            List<Map<String, Integer>> items = (List<Map<String, Integer>>) payload.get("items");

            if (bookingId == null || items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ"));
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking với ID: " + bookingId));

            // Tìm hoặc tạo BookingDetail
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            BookingDetail bookingDetail;
            if (details.isEmpty()) {
                bookingDetail = new BookingDetail();
                bookingDetail.setBooking(booking);
                bookingDetail.setRoomQuantity(1);
                bookingDetail.setPrice(0.0);
                bookingDetail.setDiscountAmount(0.0);
                bookingDetail = bookingDetailRepository.save(bookingDetail);
            } else {
                bookingDetail = details.get(0);
            }

            double totalAmount = 0.0;

            for (Map<String, Integer> item : items) {
                Integer fwbId = item.get("fwbId");
                Integer quantity = item.get("quantity");

                if (quantity == null || quantity <= 0) continue;

                FwB fwb = fwbRepository.findById(fwbId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy món với ID: " + fwbId));

                double price = extractPriceFromFwb(fwb);

                BookingFB orderItem = new BookingFB();
                orderItem.setBookingDetail(bookingDetail);
                orderItem.setFwb(fwb);
                orderItem.setQuantity(quantity);
                orderItem.setPriceAtOrder(price);
                bookingFBRepository.save(orderItem);

                totalAmount += price * quantity;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Gọi món thành công");
            response.put("totalAmount", totalAmount);
            response.put("bookingId", bookingId);
            response.put("itemCount", items.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== API LẤY ORDER THEO BOOKING =====
    @GetMapping("/booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> getOrdersByBooking(@PathVariable Integer bookingId) {
        try {
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (details.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "Chưa có đơn hàng nào", "items", new ArrayList<>()));
            }
            BookingDetail bookingDetail = details.get(0);
            List<BookingFB> orders = bookingFBRepository.findByBookingDetailId(bookingDetail.getId());
            return ResponseEntity.ok(Map.of("bookingId", bookingId, "items", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== VIEW TRANG POS =====
    @GetMapping
    public String posPage() {
        return "html/client-html/pos-order";
    }

    // ===== HELPER: LẤY GIÁ TỪ FWB =====
    private double extractPriceFromFwb(FwB fwb) {
        try {
            String description = fwb.getDescription();
            if (description != null && !description.isEmpty()) {
                Map<String, Object> data = objectMapper.readValue(description, Map.class);
                Object priceObj = data.get("price");
                if (priceObj instanceof Number) {
                    return ((Number) priceObj).doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse giá từ FwB: " + e.getMessage());
        }
        return 0.0;
    }
}