package org.example.bookinghotels.service;

import org.example.bookinghotels.entity.Hotels;

import java.util.List;

public interface HotelService {

    List<Hotels> getAllHotels();

    Hotels createHotel(Hotels hotel);

    Hotels updateHotel(Integer id, Hotels hotel);

    void deleteHotel(Integer id);
}