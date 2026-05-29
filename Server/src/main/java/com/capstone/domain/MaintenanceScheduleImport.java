package com.capstone.domain;

import com.capstone.models.MaintMileage;
import com.capstone.models.MaintMileageSummary;
import java.util.List;

public record MaintenanceScheduleImport(
    List<MaintMileage> maintMileages, List<MaintMileageSummary> summaries) {}
