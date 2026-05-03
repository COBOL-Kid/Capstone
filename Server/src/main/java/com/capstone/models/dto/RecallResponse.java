package com.capstone.models.dto;

import java.time.LocalDate;

public record RecallResponse(
        Long recallId,
        String vin,
        String nhtsaCampaignNumber,
        LocalDate reportReceivedDate,
        String component,
        String summary,
        String consequence,
        String remedy) {
}
