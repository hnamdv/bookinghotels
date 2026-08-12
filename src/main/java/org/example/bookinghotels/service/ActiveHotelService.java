package org.example.bookinghotels.service;

import jakarta.servlet.http.HttpSession;
import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.repository.HotelsRepository;
import org.springframework.stereotype.Service;

@Service
public class ActiveHotelService {

    private static final String ACTIVE_HOTEL_ID = "activeHotelId";

    private final HotelsRepository hotelsRepository;
    private final HttpSession session;

    public ActiveHotelService(HotelsRepository hotelsRepository,
                              HttpSession session) {
        this.hotelsRepository = hotelsRepository;
        this.session = session;
    }

    // =====================================================
    // CHỌN CHI NHÁNH
    // =====================================================
    public void setActiveHotel(Integer hotelId) {

        Hotels hotel = hotelsRepository.findById(hotelId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy chi nhánh với ID: " + hotelId
                        )
                );

        session.setAttribute(ACTIVE_HOTEL_ID, hotel.getId());
    }

    // =====================================================
    // LẤY ID CHI NHÁNH ĐANG ACTIVE
    // =====================================================
    public Integer getActiveHotelId() {

        Object value = session.getAttribute(ACTIVE_HOTEL_ID);

        if (value == null) {
            return null;
        }

        return (Integer) value;
    }

    // =====================================================
    // LẤY CHI NHÁNH ĐANG ACTIVE
    // =====================================================
    public Hotels getActiveHotel() {

        Integer hotelId = getActiveHotelId();

        if (hotelId == null) {
            return null;
        }

        return hotelsRepository.findById(hotelId)
                .orElse(null);
    }

    // =====================================================
    // XÓA CHI NHÁNH ACTIVE
    // =====================================================
    public void clearActiveHotel() {
        session.removeAttribute(ACTIVE_HOTEL_ID);
    }
}