package com.skillgapai.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.skillgapai.domain.RoadmapTask;
import com.skillgapai.domain.User;
import com.skillgapai.repository.RoadmapTaskRepository;
import com.skillgapai.repository.SkillRatingRepository;
import com.skillgapai.repository.UserRepository;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    private final RoadmapTaskRepository tasks;
    private final SkillRatingRepository ratings;
    private final UserRepository users;

    public ProgressController(RoadmapTaskRepository tasks, SkillRatingRepository ratings, UserRepository users) {
        this.tasks = tasks;
        this.ratings = ratings;
        this.users = users;
    }

    @GetMapping
    public ProgressResponse getProgress(Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<RoadmapTask> userTasks = tasks.findAllByUserIdOrderByCreatedAt(user.getId());
        int completed = (int) userTasks.stream().filter(RoadmapTask::isCompleted).count();
        int estimatedMinutesCompleted = userTasks.stream()
                .filter(RoadmapTask::isCompleted)
                .mapToInt(RoadmapTask::getEstimatedMinutes)
                .sum();
        int percent = userTasks.isEmpty() ? 0 : Math.round(completed * 100f / userTasks.size());

        return new ProgressResponse(
                ratings.findAllByUserIdOrderBySkillName(user.getId()).size() == AssessmentController.SKILLS.size(),
                completed,
                userTasks.size(),
                percent,
                estimatedMinutesCompleted / 60.0,
                userTasks.stream().filter(task -> !task.isCompleted()).map(RoadmapTask::getSkill).distinct().toList());
    }

    public record ProgressResponse(boolean assessmentComplete, int completedModules, int totalModules,
            int completionPercent, double estimatedHoursCompleted, List<String> remainingSkills) {}
}