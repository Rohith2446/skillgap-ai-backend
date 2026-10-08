package com.skillgapai.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap_tasks")
public class RoadmapTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 80)
    private String skill;

    @Column(nullable = false)
    private boolean completed;

    @Column
    private Integer priority;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RoadmapTask() {
    }

    public RoadmapTask(User user, String title, String skill) {
        this.user = user;
        this.title = title;
        this.skill = skill;
        this.priority = 3;
        this.estimatedMinutes = 45;
    }

    public RoadmapTask(User user, String title, String skill, int priority, int estimatedMinutes) {
        this.user = user;
        this.title = title;
        this.skill = skill;
        this.priority = priority;
        this.estimatedMinutes = estimatedMinutes;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSkill() { return skill; }
    public boolean isCompleted() { return completed; }
    public int getPriority() { return priority == null ? 3 : priority; }
    public int getEstimatedMinutes() { return estimatedMinutes == null ? 45 : estimatedMinutes; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Instant getCreatedAt() { return createdAt; }
}