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
@Table(name = "user_canvas_binding", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_canvas_binding_user", columnNames = "user_id")
})
public class UserCanvasBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "canvas_username", nullable = false, length = 120)
    private String canvasUsername;

    @Column(name = "canvas_password", nullable = false, length = 255)
    private String canvasPassword;

    @Column(name = "session_cookies_json", columnDefinition = "json")
    private String sessionCookiesJson;

    @Column(name = "session_refreshed_at")
    private LocalDateTime sessionRefreshedAt;

    @Column(name = "course_ids_json", columnDefinition = "json")
    private String courseIdsJson;

    @Column(name = "include_todo", nullable = false)
    private Boolean includeTodo = true;

    @Column(name = "include_global_announcements", nullable = false)
    private Boolean includeGlobalAnnouncements = true;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "last_sync_status", length = 50)
    private String lastSyncStatus;

    @Column(name = "last_sync_message", length = 500)
    private String lastSyncMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
