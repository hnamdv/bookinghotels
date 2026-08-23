package org.example.bookinghotels.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collections;
import java.util.Map;

@Entity
@Table(name = "fwb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FwB {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "booking_fwb_id")
    private Integer bookingFwbId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String status;

    public String getName() {
        Object value = getJsonValue("name");
        if (value != null && !value.toString().isBlank()) return value.toString();
        if (description == null || description.trim().isBlank()) return "Dịch vụ phòng khách sạn";
        String trimmed = description.trim();
        return trimmed.startsWith("{") ? "Dịch vụ phòng khách sạn" : trimmed;
    }

    public double getPrice() {
        Object value = getJsonValue("price");
        if (value instanceof Number number) return number.doubleValue();
        if (value != null) {
            try { return Double.parseDouble(value.toString()); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    public String getUnit() {
        Object value = getJsonValue("unit");
        return value == null || value.toString().isBlank() ? "lượt" : value.toString();
    }

    public String getCategory() {
        Object value = getJsonValue("category");
        return value == null || value.toString().isBlank() ? "Dịch vụ phòng" : value.toString();
    }

    public String getImage() {
        Object value = getJsonValue("image");
        return value == null ? "" : value.toString();
    }

    private Object getJsonValue(String key) {
        Map<String, Object> map = readJsonMap();
        return map.get(key);
    }

    @Transient
    private Map<String, Object> readJsonMap() {
        if (description == null || description.trim().isEmpty()) return Collections.emptyMap();
        String trimmed = description.trim();
        if (!trimmed.startsWith("{")) return Collections.emptyMap();
        try {
            return MAPPER.readValue(trimmed, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
