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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fnb-order")
@CrossOrigin(origins = "*")
public class FnbOrderController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private FwbRepository fwbRepository;

    @Autowired
    private BookingFBRepository bookingFBRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create")
    public ResponseEntity<?> createFnbOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer bookingId = (Integer) payload.get("bookingId");
            List<Map<String, Integer>> items = (List<Map<String, Integer>>) payload.get("items");

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking với ID: " + bookingId));

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
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getOrdersByBooking(@PathVariable Integer bookingId) {
        try {
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (details.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("message", "Chưa có đơn hàng nào");
                empty.put("items", new ArrayList<>());
                return ResponseEntity.ok(empty);
            }
            BookingDetail bookingDetail = details.get(0);
            List<BookingFB> orders = bookingFBRepository.findByBookingDetailId(bookingDetail.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("bookingId", bookingId);
            response.put("items", orders);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy danh sách tất cả booking (phòng)
    @GetMapping("/bookings")
    public ResponseEntity<List<Map<String, Object>>> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Booking b : bookings) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("name", b.getName() != null ? b.getName() : "");
            item.put("phone", b.getPhone() != null ? b.getPhone() : "");
            item.put("checkinDate", b.getCheckinDate());
            item.put("checkoutDate", b.getCheckoutDate());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

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