package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HotelRoomServiceImpl implements HotelRoomService {

    @Autowired
    private HotelsRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    // --- 1. QUẢN LÝ KHÁCH SẠN (HOTELS) ---
    @Override
    public List<Hotels> getAllHotels() {
        return hotelRepository.findAll();
    }

    @Override
    public Hotels createHotel(Hotels hotel) {
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotels updateHotel(Integer id, Hotels hotelDetails) {
        Hotels hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));
        hotel.setName(hotelDetails.getName());
        hotel.setAddress(hotelDetails.getAddress());
        return hotelRepository.save(hotel);
    }

    @Override
    public void deleteHotel(Integer id) {
        hotelRepository.deleteById(id);
    }

    // --- 2. QUẢN LÝ PHÒNG VẬT LÝ (ROOM) ---
    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(Integer id, Room roomDetails) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + id));

        // Cập nhật số phòng (101, 102...)
        room.setRoomNumber(roomDetails.getRoomNumber());

        // 🛠️ Nếu Entity Room của bạn không có thuộc tính 'status', hãy tạm thời comment dòng dưới lại bằng dấu //
        // room.setStatus(roomDetails.getStatus());

        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Integer id) {
        roomRepository.deleteById(id);
    }

    // --- 3. QUẢN LÝ LỊCH TRẠNG THÁI PHÒNG ---
    @Override
    @Transactional
    public void lockRoomCalendarForBooking(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        // Viết logic xử lý khóa ngày của bạn ở đây (nếu có dùng bảng RoomAvailability)
    }

    @Override
    @Transactional
    public void updateRoomStatusManually(Integer roomId, LocalDate date, String status) {
        // Viết logic admin cập nhật trạng thái dọn dẹp/bảo trì ở đây
    }
}