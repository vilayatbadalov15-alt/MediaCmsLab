package com.example.mediacmslab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "contents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long revision;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String mediaUrl;
    private Integer durationSeconds;
    private Instant publishedAt;

    @ElementCollection
    @CollectionTable(name = "podcast_episodes", joinColumns = @JoinColumn(name = "content_id"))
    @OrderColumn(name = "episode_position")
    @Column(name = "title", nullable = false)
    private List<String> episodes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToMany
    @JoinTable(
            name = "content_tags",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum ContentType { ARTICLE, VIDEO, PODCAST, AUDIO, IMAGE }
    public enum Status { DRAFT, PUBLISHED, ARCHIVED }
}
