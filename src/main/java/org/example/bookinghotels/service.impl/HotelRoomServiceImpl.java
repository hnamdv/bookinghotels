package org.example.bookinghotels.service.impl;

import org.example.bookinghotels.entity.Hotels;
import org.example.bookinghotels.repository.HotelsRepository;
import org.example.bookinghotels.service.HotelRoomService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelRoomServiceImpl implements HotelRoomService {

    private final HotelsRepository hotelRepository;

    public HotelRoomServiceImpl(HotelsRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public List<Hotels> getAllHotels() {
        return hotelRepository.findAll();
    }

    @Override
    public Hotels getHotelById(Integer id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh khách sạn"));
    }

    @Override
    public Hotels createHotel(Hotels hotel) {
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotels updateHotel(Integer id, Hotels hotelDetails) {

        Hotels hotel = getHotelById(id);

        hotel.setName(hotelDetails.getName());
        hotel.setPhone(hotelDetails.getPhone());
        hotel.setEmail(hotelDetails.getEmail());
        hotel.setAddress(hotelDetails.getAddress());
        hotel.setDescription(hotelDetails.getDescription());
        hotel.setMap(hotelDetails.getMap());
        hotel.setLogo(hotelDetails.getLogo());
        hotel.setThumbnail(hotelDetails.getThumbnail());
        hotel.setSlug(hotelDetails.getSlug());

        return hotelRepository.save(hotel);
    }

    @Override
    public void deleteHotel(Integer id) {
        hotelRepository.deleteById(id);
    }
}