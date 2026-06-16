package com.campus.campus_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String teacherName;

    @Column(nullable = false, unique = true, length = 50)
    private String courseCode;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 50)
    private String semester;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(length = 255)
    private String tags;

    @Column(nullable = false)
    private Integer memberCount = 0;

    @Column(nullable = false)
    private Integer questionCount = 0;

    @Column(nullable = false)
    private Integer experienceCount = 0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
