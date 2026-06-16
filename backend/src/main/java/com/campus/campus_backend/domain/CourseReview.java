package com.campus.campus_backend.domain;

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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "course_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_review_user", columnNames = { "course_id", "user_id" })
})
public class CourseReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer difficultyRating;

    @Column(nullable = false)
    private Integer workloadRating;

    @Column(nullable = false)
    private Integer gainRating;

    @Column(nullable = false)
    private Integer clarityRating;

    @Column(nullable = false)
    private Integer examPressureRating;

    @Column(nullable = false)
    private Integer recommendRating;

    @Column(columnDefinition = "text")
    private String content;

    @Column(length = 255)
    private String tags;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
