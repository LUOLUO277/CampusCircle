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
@Table(name = "aggregated_notice", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aggregated_notice_source_external_owner", columnNames = {
                "source_id", "external_id", "owner_user_id" })
})
public class AggregatedNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private SubscriptionSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(length = 50)
    private String category;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "original_url", length = 500)
    private String originalUrl;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column
    private LocalDateTime deadline;

    @Column(name = "target_audience", length = 255)
    private String targetAudience;

    @Column(length = 255)
    private String location;

    @Column(name = "action_links_json", columnDefinition = "json")
    private String actionLinksJson;

    @Column(name = "tags_json", columnDefinition = "json")
    private String tagsJson;

    @Column(name = "content_snapshot", columnDefinition = "text")
    private String contentSnapshot;

    @Column(name = "raw_payload_json", columnDefinition = "json")
    private String rawPayloadJson;

    @Column(name = "extraction_status", length = 50)
    private String extractionStatus;

    @Column(nullable = false, length = 20)
    private String status = "ONLINE";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
