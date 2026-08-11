package org.example.bookinghotels.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.FwbRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FwBService {

    private final FwbRepository fwbRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FwBService(FwbRepository fwbRepository) {
        this.fwbRepository = fwbRepository;
    }

    public List<Map<String, Object>> getAll() {
        return fwbRepository.findAll().stream()
                .sorted(Comparator.comparing(FwB::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toMap)
                .toList();
    }


    public List<Map<String, Object>> getRoomAmenityOptions() {
        return getAll().stream()
                .filter(item -> isRoomAmenity(item))
                .filter(item -> Number.class.isInstance(item.get("price")) ? ((Number) item.get("price")).doubleValue() <= 0D : true)
                .filter(item -> isVisible(item))
                .toList();
    }

    public List<Map<String, Object>> getChargeableServices() {
        return getAll().stream()
                .filter(item -> !isRoomAmenity(item))
                .filter(item -> Number.class.isInstance(item.get("price")) && ((Number) item.get("price")).doubleValue() > 0D)
                .filter(item -> isVisible(item))
                .toList();
    }

    public boolean isRoomAmenity(Map<String, Object> item) {
        Object category = item.get("category");
        String raw = category == null ? "" : category.toString().toLowerCase(Locale.ROOT);
        return raw.contains("tiện ích") || raw.contains("tien ich") || raw.contains("amenity");
    }

    private boolean isVisible(Map<String, Object> item) {
        Object status = item.get("status");
        return status == null || !"hidden".equalsIgnoreCase(status.toString());
    }

    public List<FwB> findAll() {
        return fwbRepository.findAll();
    }

    public List<FwB> findActiveServices() {
        return fwbRepository.findAll().stream()
                .filter(f -> f.getStatus() == null
                        || f.getStatus().isBlank()
                        || "ACTIVE".equalsIgnoreCase(f.getStatus())
                        || "SHOW".equalsIgnoreCase(f.getStatus()))
                .toList();
    }

    public Optional<FwB> findById(Integer id) {
        return fwbRepository.findById(id);
    }

    @Transactional
    public FwB saveService(Integer id, String name, Double price, String unit, String image, String category, String status) {
        FwB fwb = id == null ? new FwB() : fwbRepository.findById(id).orElse(new FwB());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", blankToDefault(name, "Dịch vụ phòng"));
        payload.put("price", price == null ? 0D : price);
        payload.put("unit", blankToDefault(unit, "lượt"));
        payload.put("image", image == null ? "" : image.trim());
        payload.put("category", blankToDefault(category, "Dịch vụ phòng"));
        try {
            fwb.setDescription(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            fwb.setDescription(payload.get("name") + " - " + payload.get("price"));
        }
        fwb.setStatus(blankToDefault(status, "ACTIVE"));
        return fwbRepository.saveAndFlush(fwb);
    }

    @Transactional
    public void hide(Integer id) {
        fwbRepository.findById(id).ifPresent(f -> {
            f.setStatus("hidden");
            fwbRepository.saveAndFlush(f);
        });
    }

    public Map<String, Object> toMap(FwB fwb) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", fwb.getId());
        map.put("name", fwb.getName());
        map.put("price", fwb.getPrice());
        map.put("status", fwb.getStatus() == null ? "ACTIVE" : fwb.getStatus());
        map.put("unit", "lượt");
        map.put("image", "");
        map.put("category", "Dịch vụ phòng");

        String description = fwb.getDescription();
        if (description != null && description.trim().startsWith("{")) {
            try {
                Map<String, Object> json = objectMapper.readValue(description, new TypeReference<Map<String, Object>>() {});
                for (String key : List.of("name", "price", "unit", "image", "category")) {
                    if (json.containsKey(key) && json.get(key) != null) {
                        map.put(key, json.get(key));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.trim().isBlank() ? fallback : value.trim();
    }
}
