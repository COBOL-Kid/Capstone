package com.capstone.data.read;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

final class ImageUrlsParser {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private ImageUrlsParser() {}

  static List<String> parse(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return new ArrayList<>(JSON.readValue(json, STRING_LIST));
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to deserialize image URLs", ex);
    }
  }
}
