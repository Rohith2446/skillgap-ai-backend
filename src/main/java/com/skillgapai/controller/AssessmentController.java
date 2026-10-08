package com.skillgapai.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillgapai.domain.SkillRating;
import com.skillgapai.domain.User;
import com.skillgapai.repository.SkillRatingRepository;
import com.skillgapai.repository.UserRepository;

@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {
    static final List<String> SKILLS = List.of("Java", "SQL", "Communication", "Problem solving", "Data analysis", "Cloud fundamentals");
    private final SkillRatingRepository ratings;
    private final UserRepository users;

    public AssessmentController(SkillRatingRepository ratings, UserRepository users) {
        this.ratings = ratings;
        this.users = users;
    }

    @GetMapping
    public List<SkillResponse> get(Authentication authentication) {
        User user = currentUser(authentication);
        return ratings.findAllByUserIdOrderBySkillName(user.getId()).stream()
                .map(rating -> new SkillResponse(rating.getSkillName(), rating.getRating())).toList();
    }

    @PutMapping
    @Transactional
    public List<SkillResponse> save(Authentication authentication, @Valid @RequestBody Map<String, Integer> scores) {
        User user = currentUser(authentication);
        if (!SKILLS.containsAll(scores.keySet()) || scores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessment contains an unknown skill");
        }
        if (!scores.keySet().containsAll(SKILLS) || scores.size() != SKILLS.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rate each supported skill before saving the assessment");
        }
        if (scores.values().stream().anyMatch(score -> score == null || score < 0 || score > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill ratings must be between 0 and 5");
        }
        Map<String, SkillRating> existingRatings = ratings.findAllByUserIdOrderBySkillName(user.getId()).stream()
            .collect(Collectors.toMap(SkillRating::getSkillName, Function.identity()));
        List<SkillRating> updatedRatings = scores.entrySet().stream()
            .map(entry -> {
                SkillRating rating = existingRatings.get(entry.getKey());
                if (rating == null) {
                return new SkillRating(user, entry.getKey(), entry.getValue());
                }
                rating.setRating(entry.getValue());
                return rating;
            })
            .toList();
        return ratings.saveAll(updatedRatings)
                .stream().map(rating -> new SkillResponse(rating.getSkillName(), rating.getRating())).toList();
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record SkillResponse(String skill, int rating) {}
}