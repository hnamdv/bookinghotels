package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.FavoriteRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FavoriteRoomRepository extends JpaRepository<FavoriteRoom, Integer> {

    boolean existsByOwnerKeyAndRoomType_Id(String ownerKey, Integer roomTypeId);

    long countByOwnerKey(String ownerKey);

    @Query("select f.roomType.id from FavoriteRoom f where f.ownerKey = :ownerKey order by f.createdAt desc")
    List<Integer> findRoomTypeIdsByOwnerKey(@Param("ownerKey") String ownerKey);

    @Transactional
    void deleteByOwnerKeyAndRoomType_Id(String ownerKey, Integer roomTypeId);

    @Transactional
    void deleteByOwnerKey(String ownerKey);
}
