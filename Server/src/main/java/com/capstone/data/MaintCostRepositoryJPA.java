package com.capstone.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.models.MaintCost;

public interface MaintCostRepositoryJPA extends JpaRepository<MaintCost, Long> {
}
