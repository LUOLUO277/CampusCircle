package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "subscription_source", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_source_source_key", columnNames = "source_key")
})
public class SubscriptionSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "source_key", nullable = false, length = 100)
    private String sourceKey;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "search_keywords", length = 500)
    private String searchKeywords;

    @Column(name = "fetch_strategy", length = 50)
    private String fetchStrategy;

    @Column(name = "fetch_config_json", columnDefinition = "json")
    private String fetchConfigJson;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "last_fetch_status", length = 50)
    private String lastFetchStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
