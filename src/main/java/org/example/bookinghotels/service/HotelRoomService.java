package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Room;
import java.time.LocalDate;
import java.util.List;

public interface HotelRoomService {

    // === 1. Khai báo các hàm cho Khách Sạn (Hotels) ===
    List<Hotels> getAllHotels();
    Hotels createHotel(Hotels hotel);
    Hotels updateHotel(Integer id, Hotels hotelDetails);
    void deleteHotel(Integer id);

    // === 2. Khai báo các hàm cho Phòng Vật Lý (Room) ===
    List<Room> getAllRooms();
    Room createRoom(Room room);
    Room updateRoom(Integer id, Room roomDetails);
    void deleteRoom(Integer id);

    // === 3. Khai báo các hàm quản lý lịch trạng thái phòng ===
    void lockRoomCalendarForBooking(Integer roomId, LocalDate checkIn, LocalDate checkOut);
    void updateRoomStatusManually(Integer roomId, LocalDate date, String status);
}