package com.example.mediacmslab.dto;

import com.example.mediacmslab.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentResponse {
    private Long id;
    private String title;
    private String description;
    private Content.ContentType type;
    private String body;
    private Content.Status status;
    private String authorEmail;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
