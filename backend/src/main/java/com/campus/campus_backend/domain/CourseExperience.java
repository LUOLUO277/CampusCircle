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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "course_experiences")
public class CourseExperience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String semester;

    @Column(nullable = false, length = 100)
    private String teacherName;

    @Column(nullable = false)
    private Integer difficultyRating;

    @Column(nullable = false)
    private Integer workloadRating;

    @Column(nullable = false)
    private Integer gainRating;

    @Column(nullable = false)
    private Integer examPressureRating;

    @Column(nullable = false)
    private Integer recommendRating;

    @Column(nullable = false, columnDefinition = "text")
    private String studyAdvice;

    @Column(nullable = false, columnDefinition = "text")
    private String pitfallAdvice;

    @Column(nullable = false, columnDefinition = "text")
    private String suitableFor;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
