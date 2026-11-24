package com.example.mediacmslab.dto;

import com.example.mediacmslab.entity.Content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Instant publishedAt;
    private Long id;
    private String title;
    private String description;
    private Content.ContentType type;
    private String body;
    private String mediaUrl;
    private Integer durationSeconds;
    private List<String> episodes;
    private Content.Status status;
    private String authorEmail;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
