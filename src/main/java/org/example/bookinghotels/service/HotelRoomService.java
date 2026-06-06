package org.example.bookinghotels.service;
import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;

import java.util.List;

public interface HotelRoomService {
    List<Hotels> getAllHotels();
    Hotels getHotelBySlug(String slug);

    List<RoomType> getRoomTypesByHotel(Integer hotelId);
    RoomType createRoomType(RoomType roomType);

    List<Room> getRoomsByTypeId(Integer roomTypeId);
    RoomImg addRoomImage(RoomImg roomImg);
}