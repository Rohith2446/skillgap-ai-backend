package com.skillgapai.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillgapai.domain.SkillRating;
import com.skillgapai.domain.User;
import com.skillgapai.repository.SkillRatingRepository;
import com.skillgapai.repository.UserRepository;
import com.skillgapai.service.InterviewPrepService;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {
    private final InterviewPrepService interviewPrep;
    private final SkillRatingRepository ratings;
    private final UserRepository users;

    public InterviewController(InterviewPrepService interviewPrep, SkillRatingRepository ratings, UserRepository users) {
        this.interviewPrep = interviewPrep;
        this.ratings = ratings;
        this.users = users;
    }

    @GetMapping("/questions")
    public List<InterviewPrepService.Question> questions(Authentication authentication) {
        User user = currentUser(authentication);
        List<SkillRating> userRatings = ratings.findAllByUserIdOrderBySkillName(user.getId());
        if (userRatings.size() != AssessmentController.SKILLS.size()) {
            return List.of();
        }
        List<String> gaps = userRatings.stream()
                .filter(rating -> rating.getRating() <= 3)
                .sorted(java.util.Comparator.comparingInt(SkillRating::getRating))
                .limit(3)
                .map(SkillRating::getSkillName)
                .toList();
        return interviewPrep.questions(user.getTargetRole(), gaps);
    }

    @PostMapping("/evaluate")
    public InterviewPrepService.Feedback evaluate(Authentication authentication, @Valid @RequestBody AnswerRequest request) {
        currentUser(authentication);
        return interviewPrep.evaluate(request.answer());
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record AnswerRequest(@NotBlank @Size(max = 5000) String answer) {}
}