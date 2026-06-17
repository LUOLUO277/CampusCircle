package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ai_retrieved_sources")
public class AiRetrievedSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private AiChatMessage message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiSourceType sourceType;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 800)
    private String summary;

    @Column(nullable = false)
    private Double score = 0D;

    @Column(name = "published_at", length = 50)
    private String publishedAt;

    @Column(name = "source_route", length = 255)
    private String sourceRoute;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
