package com.capstone.data;

import com.capstone.models.MaintMileageSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintMileageSummaryRepositoryJPA
    extends JpaRepository<MaintMileageSummary, Long> {}
