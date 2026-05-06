package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notice_subscription", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notice_subscription_user_source", columnNames = { "user_id", "source_id" })
})
public class NoticeSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private SubscriptionSource source;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "keyword_rules_json", columnDefinition = "json")
    private String keywordRulesJson;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
