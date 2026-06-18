package org.example.bookinghotels.entity;

import jakarta.persistence.*; // Import để nhận diện @Entity, @Table, @Id, @GeneratedValue
import lombok.AllArgsConstructor;
import lombok.Getter;           // Bổ sung import Getter chuẩn của Lombok
import lombok.NoArgsConstructor;
import lombok.Setter;           // Bổ sung import Setter chuẩn của Lombok

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_name")
    private String fileName;    // Tên file lưu trên ổ đĩa (đã mã hóa chống trùng)

    @Column(name = "file_type")
    private String fileType;    // Kiểu file: image/png, image/jpeg

    @Column(name = "upload_path")
    private String uploadPath;  // Đường dẫn vật lý trên ổ cứng server

    @Column(name = "file_url")
    private String fileUrl;     // URL để frontend gọi (Ví dụ: /uploads/abc.jpg)
}