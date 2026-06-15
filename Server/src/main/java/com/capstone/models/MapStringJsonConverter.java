package com.capstone.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class MapStringJsonConverter implements AttributeConverter<Map<String, String>, String> {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    try {
      return JSON.writeValueAsString(attribute != null ? attribute : Map.of());
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to serialize warranty coverages", ex);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return new LinkedHashMap<>(JSON.readValue(dbData, STRING_MAP));
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to deserialize warranty coverages", ex);
    }
  }
}
