package com.capstone.data.read;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

final class ImageUrlsParser {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private ImageUrlsParser() {}

  static List<String> parse(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return new ArrayList<>(OBJECT_MAPPER.readValue(json, STRING_LIST));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize image URLs", ex);
    }
  }
}
