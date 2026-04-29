package com.capstone.integration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecallResponse(List<RecallItem> data) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RecallItem(String manufacturer, @JsonProperty("nhtsaCampaignNumber") String nhtsaCampaignNumber,
			Boolean parkIt, @JsonProperty("parkOutSide") Boolean parkOutside, Boolean overTheAirUpdate,
			@JsonProperty("reportReceivedDate") String reportReceivedDate, String component, String summary,
			String consequence, String remedy, String notes, String modelYear, String make, String model) {
	}
}
