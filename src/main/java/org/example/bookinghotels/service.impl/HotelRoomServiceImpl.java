package org.example.bookinghotels.service.impl;


import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.entity.Room;
import org.example.bookinghotels.entity.RoomImg;
import org.example.bookinghotels.entity.RoomType;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.repository.RoomImgRepository;
import org.example.bookinghotels.repository.RoomRepository;
import org.example.bookinghotels.repository.RoomTypeRepository;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelRoomServiceImpl implements HotelRoomService {

    @Autowired private HotelsRepository hotelsRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomImgRepository roomImgRepository;

    @Override
    public List<Hotels> getAllHotels() {
        return hotelsRepository.findAll();
    }

    @Override
    public Hotels getHotelBySlug(String slug) {
        return hotelsRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn phù hợp slug"));
    }

    @Override
    public List<RoomType> getRoomTypesByHotel(Integer hotelId) {
        return roomTypeRepository.findAll().stream()
                .filter(rt -> rt.getHotels().getId().equals(hotelId))
                .collect(Collectors.toList());
    }

    @Override
    public RoomType createRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    @Override
    public List<Room> getRoomsByTypeId(Integer roomTypeId) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getRoomType().getId().equals(roomTypeId))
                .collect(Collectors.toList());
    }

    @Override
    public RoomImg addRoomImage(RoomImg roomImg) {
        return roomImgRepository.save(roomImg);
    }
}