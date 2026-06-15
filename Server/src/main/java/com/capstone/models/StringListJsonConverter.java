package com.capstone.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      return JSON.writeValueAsString(attribute != null ? attribute : List.of());
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to serialize image URLs", ex);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return new ArrayList<>(JSON.readValue(dbData, STRING_LIST));
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to deserialize image URLs", ex);
    }
  }
}
