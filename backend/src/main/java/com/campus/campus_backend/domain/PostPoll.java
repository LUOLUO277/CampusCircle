package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "post_polls", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_polls_post", columnNames = "post_id")
})
public class PostPoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "multiple", nullable = false)
    private Boolean multiple = false;

    @Column(name = "total_votes", nullable = false)
    private Integer totalVotes = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<PostPollOption> options;
}

