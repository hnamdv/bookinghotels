package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.Booking;
import org.example.bookinghotels.entity.BookingDetail;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.BookingDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test chống gối lịch - Overbooking")
class BookingServiceImplTest {

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private static final Integer ROOM_101 = 1;
    private static final Integer ROOM_102 = 2;
    private static final Integer ROOM_103 = 3;

    // Booking cũ trong DB: 10/6/2026 -> 15/6/2026
    private static final LocalDate EXISTING_CHECKIN = LocalDate.of(2026, 6, 10);
    private static final LocalDate EXISTING_CHECKOUT = LocalDate.of(2026, 6, 15);

    // ============================================================
    // TEST: isRoomAvailable() - Kiểm tra phòng trống
    // ============================================================
    @Nested
    @DisplayName("Kiểm tra phòng trống")
    class IsRoomAvailableTests {

        @Test
        @DisplayName("✅ Phòng trống - Không có booking nào")
        void shouldReturnTrueWhenNoBookings() {
            when(bookingDetailRepository.existsOverlappingBooking(anyInt(), any(), any()))
                    .thenReturn(false);

            boolean result = bookingService.isRoomAvailable(ROOM_101,
                    LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 25));

            assertTrue(result);
        }

        @Test
        @DisplayName("❌ Phòng đã đặt - Có booking gối lịch")
        void shouldReturnFalseWhenOverlapping() {
            when(bookingDetailRepository.existsOverlappingBooking(anyInt(), any(), any()))
                    .thenReturn(true);

            boolean result = bookingService.isRoomAvailable(ROOM_101,
                    LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 14));

            assertFalse(result);
        }
    }

    // ============================================================
    // TEST: getAvailableRooms() - Loại trừ phòng gối lịch
    // ============================================================
    @Nested
    @DisplayName("Loại trừ phòng gối lịch")
    class GetAvailableRoomsTests {

        @Test
        @DisplayName("Tất cả phòng trống -> Trả về toàn bộ")
        void shouldReturnAllRoomsWhenNoneBooked() {
            List<Integer> allRoomIds = Arrays.asList(ROOM_101, ROOM_102, ROOM_103);
            when(bookingDetailRepository.findOverlappingBookings(anyList(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Integer> result = bookingService.getAvailableRooms(allRoomIds,
                    LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 25));

            assertEquals(3, result.size());
            assertTrue(result.containsAll(allRoomIds));
        }

        @Test
        @DisplayName("1 phòng gối lịch -> Loại bỏ phòng đó")
        void shouldExcludeBookedRoom() {
            BookingDetail booked = createMockBookingDetail(ROOM_101, EXISTING_CHECKIN, EXISTING_CHECKOUT);
            List<Integer> allRoomIds = Arrays.asList(ROOM_101, ROOM_102, ROOM_103);

            when(bookingDetailRepository.findOverlappingBookings(anyList(), any(), any()))
                    .thenReturn(Arrays.asList(booked));

            List<Integer> result = bookingService.getAvailableRooms(allRoomIds,
                    LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 14));

            assertEquals(2, result.size());
            assertFalse(result.contains(ROOM_101));
            assertTrue(result.contains(ROOM_102));
            assertTrue(result.contains(ROOM_103));
        }
    }

    // ============================================================
    // TEST: TẤT CẢ CASE GỐI LỊCH & KHÔNG GỐI LỊCH
    // ============================================================
    @Nested
    @DisplayName("Tất cả case gối lịch")
    class AllOverlapCases {

        static Stream<Arguments> overlapCases() {
            return Stream.of(
                    // ======== CASE GỐI LỊCH ========
                    Arguments.of(LocalDate.of(2026, 6, 8),  LocalDate.of(2026, 6, 12), true,  "Gối trái"),
                    Arguments.of(LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 13), true,  "Nằm trong"),
                    Arguments.of(LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 17), true,  "Gối phải"),
                    Arguments.of(LocalDate.of(2026, 6, 8),  LocalDate.of(2026, 6, 17), true,  "Bọc ngoài"),
                    Arguments.of(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13), true,  "Trùng checkin"),
                    Arguments.of(LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 15), true,  "Trùng checkout"),
                    Arguments.of(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 15), true,  "Trùng hoàn toàn"),

                    // ======== CASE KHÔNG GỐI LỊCH ========
                    Arguments.of(LocalDate.of(2026, 6, 5),  LocalDate.of(2026, 6, 10), false, "Liền kề trước"),
                    Arguments.of(LocalDate.of(2026, 6, 5),  LocalDate.of(2026, 6, 9),  false, "Trước hoàn toàn"),
                    Arguments.of(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 20), false, "Liền kề sau"),
                    Arguments.of(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 20), false, "Sau hoàn toàn")
            );
        }

        @ParameterizedTest(name = "[{3}] checkin={0}, checkout={1}")
        @MethodSource("overlapCases")
        void testOverlap(LocalDate newCheckin, LocalDate newCheckout, boolean expectedOverlap, String description) {
            when(bookingDetailRepository.existsOverlappingBooking(anyInt(), any(), any()))
                    .thenAnswer(invocation -> {
                        LocalDate checkin = invocation.getArgument(1);
                        LocalDate checkout = invocation.getArgument(2);
                        return checkin.isBefore(EXISTING_CHECKOUT) && checkout.isAfter(EXISTING_CHECKIN);
                    });

            boolean available = bookingService.isRoomAvailable(ROOM_101, newCheckin, newCheckout);

            if (expectedOverlap) {
                assertFalse(available, "❌ Phải phát hiện GỐI LỊCH: " + description);
            } else {
                assertTrue(available, "✅ Phải báo TRỐNG: " + description);
            }
        }
    }

    // ============================================================
    // TEST: validateBooking()
    // ============================================================
    @Nested
    @DisplayName("Validate booking")
    class ValidateBookingTests {

        @Test
        @DisplayName("❌ Checkin = Checkout -> Throw Exception")
        void shouldThrowWhenCheckinEqualsCheckout() {
            assertThrows(IllegalArgumentException.class, () -> {
                bookingService.validateBooking(ROOM_101, EXISTING_CHECKIN, EXISTING_CHECKIN);
            });
        }

        @Test
        @DisplayName("❌ Checkin sau Checkout -> Throw Exception")
        void shouldThrowWhenCheckinAfterCheckout() {
            assertThrows(IllegalArgumentException.class, () -> {
                bookingService.validateBooking(ROOM_101, EXISTING_CHECKOUT, EXISTING_CHECKIN);
            });
        }

        @Test
        @DisplayName("❌ Checkin trong quá khứ -> Throw Exception")
        void shouldThrowWhenCheckinInPast() {
            assertThrows(IllegalArgumentException.class, () -> {
                bookingService.validateBooking(ROOM_101, LocalDate.now().minusDays(1), EXISTING_CHECKOUT);
            });
        }

        @Test
        @DisplayName("❌ Phòng gối lịch -> Throw Exception")
        void shouldThrowWhenRoomOverlapping() {
            when(bookingDetailRepository.existsOverlappingBooking(anyInt(), any(), any()))
                    .thenReturn(true);

            assertThrows(IllegalStateException.class, () -> {
                bookingService.validateBooking(ROOM_101,
                        LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 14));
            });
        }

        @Test
        @DisplayName("✅ Hợp lệ -> Không throw Exception")
        void shouldNotThrowWhenValid() {
            when(bookingDetailRepository.existsOverlappingBooking(anyInt(), any(), any()))
                    .thenReturn(false);

            assertDoesNotThrow(() -> {
                bookingService.validateBooking(ROOM_101,
                        LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 25));
            });
        }
    }

    // ============================================================
    // HELPER
    // ============================================================
    private BookingDetail createMockBookingDetail(Integer roomId, LocalDate checkin, LocalDate checkout) {
        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber("10" + roomId);

        Booking booking = new Booking();
        booking.setCheckinDate(checkin);
        booking.setCheckoutDate(checkout);

        BookingDetail detail = new BookingDetail();
        detail.setRoom(room);
        detail.setBooking(booking);

        return detail;
    }
}