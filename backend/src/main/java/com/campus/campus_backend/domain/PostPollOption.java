package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "post_poll_options")
public class PostPollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private PostPoll poll;

    @Column(nullable = false, length = 255)
    private String text;

    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;
}

