package com.capstone.integration.vehicledatabases;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaintenanceScheduleResponse(String status, MaintenanceScheduleData data) {

	public record MaintenanceScheduleData(String vin, Integer year, String make, String model, String trim,
			List<MaintenanceInterval> maintenance) {
	}

	public record MaintenanceInterval(Mileage mileage, @JsonProperty("service_items") List<String> serviceItems) {
	}

	public record Mileage(Integer miles, Integer km) {
	}
}
