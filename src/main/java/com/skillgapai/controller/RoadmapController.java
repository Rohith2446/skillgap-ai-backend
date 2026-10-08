package com.skillgapai.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillgapai.domain.RoadmapTask;
import com.skillgapai.domain.SkillRating;
import com.skillgapai.domain.User;
import com.skillgapai.repository.RoadmapTaskRepository;
import com.skillgapai.repository.SkillRatingRepository;
import com.skillgapai.repository.UserRepository;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {
    private final RoadmapTaskRepository tasks;
    private final UserRepository users;
    private final SkillRatingRepository ratings;

    public RoadmapController(RoadmapTaskRepository tasks, UserRepository users, SkillRatingRepository ratings) {
        this.tasks = tasks;
        this.users = users;
        this.ratings = ratings;
    }

    @GetMapping
    public List<TaskResponse> list(Authentication authentication) {
        User user = currentUser(authentication);
        return tasks.findAllByUserIdOrderByCreatedAt(user.getId()).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/generate")
    public List<TaskResponse> generate(Authentication authentication) {
        User user = currentUser(authentication);
        List<SkillRating> userRatings = ratings.findAllByUserIdOrderBySkillName(user.getId());
        if (userRatings.size() != AssessmentController.SKILLS.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete every skill rating before generating a roadmap");
        }

        Set<String> plannedSkills = tasks.findAllByUserIdOrderByCreatedAt(user.getId()).stream()
                .map(RoadmapTask::getSkill)
                .collect(Collectors.toSet());
        List<SkillRating> gaps = userRatings.stream()
                .filter(rating -> rating.getRating() <= 3)
                .sorted(java.util.Comparator.comparingInt(SkillRating::getRating))
                .limit(3)
                .toList();

        for (int index = 0; index < gaps.size(); index++) {
            SkillRating gap = gaps.get(index);
            if (!plannedSkills.contains(gap.getSkillName())) {
                int effort = 60 + (gap.getRating() * 30);
                String title = "Build " + gap.getSkillName() + " for " + user.getTargetRole();
                tasks.save(new RoadmapTask(user, title, gap.getSkillName(), index + 1, effort));
            }
        }

        return tasks.findAllByUserIdOrderByCreatedAt(user.getId()).stream().map(TaskResponse::from).toList();
    }

    @PostMapping
    public TaskResponse add(Authentication authentication, @Valid @RequestBody TaskRequest request) {
        User user = currentUser(authentication);
        return TaskResponse.from(tasks.save(new RoadmapTask(user, request.title().trim(), request.skill().trim())));
    }

    @PutMapping("/{id}")
    public TaskResponse update(Authentication authentication, @PathVariable Long id, @RequestBody UpdateTaskRequest request) {
        User user = currentUser(authentication);
        RoadmapTask task = tasks.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        task.setCompleted(request.completed());
        return TaskResponse.from(tasks.save(task));
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record TaskRequest(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 80) String skill) {}
    public record UpdateTaskRequest(boolean completed) {}
    public record TaskResponse(Long id, String title, String skill, boolean completed, int priority, int estimatedMinutes) {
        static TaskResponse from(RoadmapTask task) {
            return new TaskResponse(task.getId(), task.getTitle(), task.getSkill(), task.isCompleted(), task.getPriority(), task.getEstimatedMinutes());
        }
    }
}