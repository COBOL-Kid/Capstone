package com.capstone.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.models.MaintMileage;

public interface MaintMileageRepositoryJPA extends JpaRepository<MaintMileage, Long> {
}