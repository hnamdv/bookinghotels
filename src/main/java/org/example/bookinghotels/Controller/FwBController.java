package org.example.bookinghotels.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.FwbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/fwb")
@CrossOrigin(origins = "*")
public class FwBController {

    @Autowired
    private FwbRepository fwbRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String UPLOAD_DIR = "uploads/fwb/";

    // =========================
    // GET ALL
    // =========================
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllFwB() {

        List<FwB> fwbList = fwbRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (FwB fwb : fwbList) {

            Map<String, Object> item =
                    parseDescription(fwb.getDescription());

            item.put("id", fwb.getId());
            item.put("status",
                    fwb.getStatus() == null
                            ? "active"
                            : fwb.getStatus());

            item.put("bookingFwbId",
                    fwb.getBookingFwbId());

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // =========================
    // GET BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>>
    getFwBById(@PathVariable Integer id) {

        Optional<FwB> optional =
                fwbRepository.findById(id);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FwB fwb = optional.get();

        Map<String, Object> result =
                parseDescription(fwb.getDescription());

        result.put("id", fwb.getId());
        result.put("status",
                fwb.getStatus() == null
                        ? "active"
                        : fwb.getStatus());

        result.put("bookingFwbId",
                fwb.getBookingFwbId());

        return ResponseEntity.ok(result);
    }

    // =========================
    // CREATE
    // =========================
    @PostMapping
    public ResponseEntity<?> createFwB(

            @RequestParam String name,
            @RequestParam Double price,
            @RequestParam String unit,

            @RequestParam(required = false)
            MultipartFile image,

            @RequestParam(required = false)
            String extraDescription

    ) {

        try {

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Tên không được để trống"
                        ));
            }

            if (price == null || price < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Giá không hợp lệ"
                        ));
            }

            if (unit == null || unit.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Đơn vị không được để trống"
                        ));
            }

            String imagePath = "";

            if (image != null && !image.isEmpty()) {
                imagePath = saveImage(image);
            }

            Map<String, Object> menuData =
                    new HashMap<>();

            menuData.put("name", name.trim());
            menuData.put("price", price);
            menuData.put("unit", unit.trim());
            menuData.put(
                    "description",
                    extraDescription == null
                            ? ""
                            : extraDescription.trim()
            );
            menuData.put("image", imagePath);

            FwB fwb = new FwB();

            fwb.setStatus("active");
            fwb.setDescription(
                    objectMapper.writeValueAsString(menuData)
            );

            FwB saved =
                    fwbRepository.save(fwb);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(saved);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFwB(

            @PathVariable Integer id,

            @RequestParam String name,
            @RequestParam Double price,
            @RequestParam String unit,

            @RequestParam(required = false)
            MultipartFile image,

            @RequestParam(required = false)
            String extraDescription

    ) {

        try {

            Optional<FwB> optional =
                    fwbRepository.findById(id);

            if (optional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FwB fwb = optional.get();

            Map<String, Object> oldData =
                    parseDescription(
                            fwb.getDescription()
                    );

            String oldImage =
                    (String) oldData.get("image");

            String imagePath = oldImage;

            if (image != null && !image.isEmpty()) {

                imagePath = saveImage(image);

                if (oldImage != null &&
                        !oldImage.isEmpty()) {

                    File oldFile =
                            new File("." + oldImage);

                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }
            }

            Map<String, Object> menuData =
                    new HashMap<>();

            menuData.put("name", name.trim());
            menuData.put("price", price);
            menuData.put("unit", unit.trim());
            menuData.put(
                    "description",
                    extraDescription == null
                            ? ""
                            : extraDescription.trim()
            );
            menuData.put("image", imagePath);

            fwb.setDescription(
                    objectMapper.writeValueAsString(menuData)
            );

            FwB updated =
                    fwbRepository.save(fwb);

            return ResponseEntity.ok(updated);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // =========================
    // TOGGLE STATUS
    // =========================
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(
            @PathVariable Integer id
    ) {

        Optional<FwB> optional =
                fwbRepository.findById(id);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FwB fwb = optional.get();

        if ("hidden".equals(fwb.getStatus())) {
            fwb.setStatus("active");
        } else {
            fwb.setStatus("hidden");
        }

        return ResponseEntity.ok(
                fwbRepository.save(fwb)
        );
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFwB(
            @PathVariable Integer id
    ) {

        Optional<FwB> optional =
                fwbRepository.findById(id);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FwB fwb = optional.get();

        Map<String, Object> data =
                parseDescription(
                        fwb.getDescription()
                );

        String imagePath =
                (String) data.get("image");

        if (imagePath != null &&
                !imagePath.isEmpty()) {

            File file =
                    new File("." + imagePath);

            if (file.exists()) {
                file.delete();
            }
        }

        fwbRepository.delete(fwb);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Xóa thành công"
                )
        );
    }

    // =========================
    // SAVE IMAGE
    // =========================
    private String saveImage(
            MultipartFile file
    ) throws IOException {

        String projectDir =
                System.getProperty("user.dir");

        Path uploadPath =
                Paths.get(
                        projectDir,
                        UPLOAD_DIR
                );

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename =
                file.getOriginalFilename();

        String extension = ".jpg";

        if (originalFilename != null &&
                originalFilename.contains(".")) {

            extension =
                    originalFilename.substring(
                            originalFilename.lastIndexOf(".")
                    );
        }

        String fileName =
                UUID.randomUUID()
                        + extension;

        Path target =
                uploadPath.resolve(fileName);

        file.transferTo(target.toFile());

        return "/uploads/fwb/" + fileName;
    }

    // =========================
    // JSON -> MAP
    // =========================
    private Map<String, Object>
    parseDescription(String description) {

        try {

            if (description != null &&
                    !description.isEmpty()) {

                return objectMapper.readValue(
                        description,
                        Map.class
                );
            }

        } catch (Exception ignored) {
        }

        Map<String, Object> empty =
                new HashMap<>();

        empty.put("name", "");
        empty.put("price", 0);
        empty.put("unit", "");
        empty.put("description", "");
        empty.put("image", "");

        return empty;
    }
}