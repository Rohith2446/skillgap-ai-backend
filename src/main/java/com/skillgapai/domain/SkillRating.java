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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "skill_ratings", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_name"}))
public class SkillRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "skill_name", nullable = false, length = 80)
    private String skillName;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected SkillRating() {
    }

    public SkillRating(User user, String skillName, int rating) {
        this.user = user;
        this.skillName = skillName;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public String getSkillName() { return skillName; }
    public int getRating() { return rating; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setRating(int rating) {
        this.rating = rating;
        this.updatedAt = Instant.now();
    }
}