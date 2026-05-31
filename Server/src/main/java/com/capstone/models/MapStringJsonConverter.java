package com.capstone.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class MapStringJsonConverter implements AttributeConverter<Map<String, String>, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    try {
      return OBJECT_MAPPER.writeValueAsString(attribute != null ? attribute : Map.of());
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize warranty coverages", ex);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return new LinkedHashMap<>(OBJECT_MAPPER.readValue(dbData, STRING_MAP));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize warranty coverages", ex);
    }
  }
}
