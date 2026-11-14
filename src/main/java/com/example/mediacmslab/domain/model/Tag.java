package com.example.mediacmslab.domain.model;

public record Tag(String name) {
    public Tag {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be null or blank");
        }
        name = name.trim().toLowerCase();
    }
}
