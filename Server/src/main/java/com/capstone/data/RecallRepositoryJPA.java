package com.capstone.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.models.Recall;

public interface RecallRepositoryJPA extends JpaRepository<Recall, Long> {
}
