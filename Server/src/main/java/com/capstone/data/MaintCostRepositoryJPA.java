package com.capstone.data;

import com.capstone.models.MaintCost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintCostRepositoryJPA extends JpaRepository<MaintCost, Long> {
}
