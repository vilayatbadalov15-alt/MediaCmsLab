package com.example.mediacmslab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    @Column(columnDefinition = "TEXT")
    private String body;   // текст или URL медиа

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
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum ContentType { ARTICLE, VIDEO, AUDIO, IMAGE }
    public enum Status { DRAFT, PUBLISHED, ARCHIVED }
}
