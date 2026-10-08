package com.skillgapai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillgapai.domain.RoadmapTask;

public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask, Long> {
    List<RoadmapTask> findAllByUserIdOrderByCreatedAt(Long userId);
    Optional<RoadmapTask> findByIdAndUserId(Long id, Long userId);
}