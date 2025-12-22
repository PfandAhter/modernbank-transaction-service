package com.modernbank.transaction_service.service.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FeatureVectorSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String toJson(Map<String, Double> features) {
        try {
            return objectMapper.writeValueAsString(features);
        } catch (Exception e) {
            throw new IllegalStateException("Feature vector serialize edilemedi", e);
        }
    }

    public Map<String, Double> fromJson(String json) {
        try {
            return objectMapper.readValue(
                    json, new TypeReference<Map<String, Double>>() {}
            );
        } catch (Exception e) {
            throw new IllegalStateException("Feature vector deserialize edilemedi", e);
        }
    }
}