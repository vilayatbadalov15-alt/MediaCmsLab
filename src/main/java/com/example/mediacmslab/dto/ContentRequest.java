package com.example.mediacmslab.dto;

import com.example.mediacmslab.entity.Content;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Content.ContentType type;

    private String body;

    @NotNull
    private Content.Status status;

    private Set<String> tags;
}
