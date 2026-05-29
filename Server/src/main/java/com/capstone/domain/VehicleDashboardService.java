package com.capstone.domain;

import com.capstone.models.dto.VehicleDashboardResponse;
import com.capstone.read.VehicleReadService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class VehicleDashboardService {

  private final VehicleReadService vehicleReadService;

  public VehicleDashboardService(VehicleReadService vehicleReadService) {
    this.vehicleReadService = vehicleReadService;
  }

  public Optional<VehicleDashboardResponse> findDashboardForUser(long userId, String vin) {
    return vehicleReadService.findDashboard(userId, vin);
  }
}
