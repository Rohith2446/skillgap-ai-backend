package com.skillgapai.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillgapai.domain.User;
import com.skillgapai.domain.SkillRating;
import com.skillgapai.repository.SkillRatingRepository;
import com.skillgapai.repository.UserRepository;
import com.skillgapai.service.SkillGapAnalysisService;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final SkillGapAnalysisService analysisService;
    private final UserRepository users;
    private final SkillRatingRepository ratings;

    public AnalysisController(SkillGapAnalysisService analysisService, UserRepository users, SkillRatingRepository ratings) {
        this.analysisService = analysisService;
        this.users = users;
        this.ratings = ratings;
    }

    @PostMapping
    public SkillGapAnalysisService.AnalysisResponse analyze(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        User user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        var userRatings = ratings.findAllByUserIdOrderBySkillName(user.getId());
        if (userRatings.size() != AssessmentController.SKILLS.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete every skill rating before generating analysis");
        }
        Map<String, Integer> scores = userRatings.stream()
            .collect(java.util.stream.Collectors.toMap(SkillRating::getSkillName, SkillRating::getRating));
        return analysisService.generateAnalysis(scores, user.getTargetRole());
    }
}
