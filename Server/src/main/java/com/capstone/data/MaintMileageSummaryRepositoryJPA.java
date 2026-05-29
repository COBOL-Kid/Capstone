package com.capstone.data;

import com.capstone.models.MaintMileageSummary;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintMileageSummaryRepositoryJPA extends JpaRepository<MaintMileageSummary, Long> {

  List<MaintMileageSummary> findByVehicleTypeId_VehicleTypeIdAndMileageDueIn(
      Long vehicleTypeId, Collection<Integer> mileageDues);
}
