package com.capstone.integration.vehicledatabases;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecallResponse(String status, RecallData data) {

    public record RecallData(String vin, String year, String make, String model, List<RecallItem> recall) {
    }

    public record RecallItem(@JsonProperty("campaign_id") String campaignId,
            @JsonProperty("recall_no") String recallNo, @JsonProperty("recall_date") String recallDate,
            @JsonProperty("component_affected") String componentAffected, String summary, String consequences,
            String remedy, String notes, @JsonProperty("manufacturer_name") String manufacturerName) {
    }
}