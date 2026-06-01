package com.capstone.integration.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class VehicleDataFixtures {

  public static final String BURST_TEST_VIN = "JTENU5JR6M5962554";

  private VehicleDataFixtures() {}

  public static String read(String resourceName) {
    String path = "vehicle-data-fixtures/" + resourceName;
    try (InputStream input = VehicleDataFixtures.class.getClassLoader().getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalStateException("Missing test fixture: " + path);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read fixture: " + path, ex);
    }
  }
}
