package com.capstone.read;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.capstone.data.read.JdbcRecordColumns;
import org.junit.jupiter.api.Test;

class JdbcRecordColumnsTest {

  @Test
  void convertsRecordComponentNamesToJdbcColumnLabels() {
    assertEquals(
        "available_image_urls_json", JdbcRecordColumns.toSnakeCase("availableImageUrlsJson"));
    assertEquals("maint_mileage_id", JdbcRecordColumns.toSnakeCase("maintMileageId"));
    assertEquals("nhtsa_campaign_number", JdbcRecordColumns.toSnakeCase("nhtsaCampaignNumber"));
    assertEquals("inspect", JdbcRecordColumns.toSnakeCase("inspect"));
    assertEquals("part_desc", JdbcRecordColumns.toSnakeCase("partDesc"));
  }
}
