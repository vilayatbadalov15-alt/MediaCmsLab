package com.example.mediacmslab.dto;

import com.example.mediacmslab.entity.Content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull
    private Content.ContentType type;

    private String body;
    private String mediaUrl;
    private Integer durationSeconds;
    private List<@NotBlank @Size(max = 255) String> episodes;

    @NotNull
    private Content.Status status;

    private Set<@NotBlank @Size(max = 255) String> tags;

    @AssertTrue(message = "Проверьте текст статьи, URL и длительность видео или список эпизодов подкаста")
    @JsonIgnore
    public boolean isFormatValid() {
        if (type == null) return true;
        return switch (type) {
            case ARTICLE -> body != null && !body.isBlank();
            case VIDEO -> validUrl(mediaUrl) && durationSeconds != null && durationSeconds > 0;
            case PODCAST -> validUrl(mediaUrl) && episodes != null && !episodes.isEmpty();
            case AUDIO, IMAGE -> body != null && !body.isBlank();
        };
    }

    private static boolean validUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null &&
                    ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
