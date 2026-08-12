package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.RoomType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {

    @Query("select distinct rt from RoomType rt " +
            "left join fetch rt.images " +
            "left join fetch rt.hotels")
    List<RoomType> findAllWithImages();

    @Query("select distinct rt from RoomType rt " +
            "left join fetch rt.images " +
            "left join fetch rt.hotels " +
            "where rt.id = :id")
    Optional<RoomType> findDetailById(@Param("id") Integer id);

    @Query("select distinct rt from RoomType rt " +
            "left join fetch rt.images " +
            "left join fetch rt.hotels " +
            "where rt.id in :ids")
    List<RoomType> findByIdInWithImages(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"images", "hotels"})
    Optional<RoomType> findById(Integer id);
    // =====================================================
    // LẤY LOẠI PHÒNG THEO CHI NHÁNH ĐANG ACTIVE
    // =====================================================
    @Query("""
        SELECT DISTINCT rt
        FROM RoomType rt
        LEFT JOIN FETCH rt.images
        LEFT JOIN FETCH rt.hotels
        WHERE rt.hotels.id = :hotelId
        ORDER BY rt.id
    """)
    List<RoomType> findByHotelId(@Param("hotelId") Integer hotelId);
}

