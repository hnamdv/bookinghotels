package org.example.bookinghotels.repository;

import org.example.bookinghotels.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, Integer> {
    // Interface này tạm thời không cần viết gì thêm, JpaRepository đã lo hết các hàm cơ bản rồi nha Hải
}