package com.skillgapai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillgapai.domain.SkillRating;

public interface SkillRatingRepository extends JpaRepository<SkillRating, Long> {
    List<SkillRating> findAllByUserIdOrderBySkillName(Long userId);
    void deleteAllByUserId(Long userId);
}