package org.example.bookinghotels.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.bookinghotels.entity.FwB;
import org.example.bookinghotels.repository.FwbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class FwBService {
    @Autowired
    private FwbRepository fwBRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, Object>> getAll() {
        return fwBRepository.findAll().stream()
                .map(this::toMap)
                .toList();
    }

    public List<Map<String, Object>> getByStatus(String status) {
        return fwBRepository.findByStatus(status).stream()
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> getById(Integer id) {
        return fwBRepository.findById(id)
                .map(this::toMap)
                .orElse(null);
    }

    private Map<String, Object> toMap(FwB entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("status", entity.getStatus());

        try {
            JsonNode json = mapper.readTree(entity.getDescription());
            map.put("name", json.has("name") ? json.get("name").asText() : "");
            map.put("price", json.has("price") ? json.get("price").asDouble() : 0);
            map.put("unit", json.has("unit") ? json.get("unit").asText() : "");
            map.put("image", json.has("image") ? json.get("image").asText() : "");
            map.put("category", json.has("category") ? json.get("category").asText() : "");
            map.put("description", json.has("description") ? json.get("description").asText() : "");
        } catch (Exception e) {
            map.put("name", "");
            map.put("price", 0);
            map.put("unit", "");
            map.put("image", "");
            map.put("category", "");
            map.put("description", "");
        }
        return map;
    }

    private FwB toEntity(Map<String, Object> data) {
        FwB entity = new FwB();

        if (data.containsKey("id") && data.get("id") != null) {
            entity.setId((Integer) data.get("id"));
        }

        entity.setStatus((String) data.getOrDefault("status", "active"));

        try {
            ObjectNode json = mapper.createObjectNode();
            json.put("name", data.get("name") != null ? (String) data.get("name") : "");
            json.put("price", data.get("price") != null ?
                    ((Number) data.get("price")).doubleValue() : 0);
            json.put("unit", data.get("unit") != null ? (String) data.get("unit") : "");
            json.put("image", data.get("image") != null ? (String) data.get("image") : "");
            json.put("category", data.get("category") != null ? (String) data.get("category") : "");
            json.put("description", data.get("description") != null ? (String) data.get("description") : "");
            entity.setDescription(mapper.writeValueAsString(json));
        } catch (Exception e) {
            entity.setDescription("{}");
        }
        return entity;
    }

    public Map<String, Object> save(Map<String, Object> data) {
        FwB entity = toEntity(data);
        if (entity.getStatus() == null) {
            entity.setStatus("active");
        }
        FwB saved = fwBRepository.save(entity);
        return toMap(saved);
    }

    public Map<String, Object> update(Integer id, Map<String, Object> data) {
        FwB existing = fwBRepository.findById(id).orElse(null);
        if (existing == null) return null;

        data.put("id", id);
        FwB entity = toEntity(data);
        entity.setBookingFwbId(existing.getBookingFwbId());
        entity.setStatus(data.get("status") != null ?
                (String) data.get("status") : existing.getStatus());

        FwB updated = fwBRepository.save(entity);
        return toMap(updated);
    }

    public void delete(Integer id) {
        fwBRepository.deleteById(id);
    }

    public Map<String, Object> toggleStatus(Integer id) {
        FwB entity = fwBRepository.findById(id).orElse(null);
        if (entity == null) return null;

        if ("active".equals(entity.getStatus())) {
            entity.setStatus("hidden");
        } else {
            entity.setStatus("active");
        }
        FwB updated = fwBRepository.save(entity);
        return toMap(updated);
    }
}