package org.example.bookinghotels.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class ImageController {

    @GetMapping("/uploads/fwb/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            // Đường dẫn đến file trong thư mục static
            Path filePath = Paths.get("src/main/resources/static/uploads/fwb/")
                    .resolve(filename)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Xác định loại file dựa trên đuôi
                MediaType mediaType = MediaType.IMAGE_JPEG;
                String lower = filename.toLowerCase();
                if (lower.endsWith(".png")) {
                    mediaType = MediaType.IMAGE_PNG;
                } else if (lower.endsWith(".gif")) {
                    mediaType = MediaType.IMAGE_GIF;
                } else if (lower.endsWith(".webp")) {
                    mediaType = MediaType.parseMediaType("image/webp");
                } else if (lower.endsWith(".jfif") || lower.endsWith(".jpeg") || lower.endsWith(".jpg")) {
                    mediaType = MediaType.IMAGE_JPEG;
                }

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}