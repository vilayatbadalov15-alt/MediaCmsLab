package com.example.mediacmslab.service;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.entity.Tag;
import com.example.mediacmslab.entity.User;
import com.example.mediacmslab.repository.ContentRepository;
import com.example.mediacmslab.repository.TagRepository;
import com.example.mediacmslab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional
    public ContentResponse create(ContentRequest request, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new RuntimeException("Автор не найден"));

        Set<Tag> tags = processTags(request.getTags());

        Content content = Content.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .body(request.getBody())
                .status(request.getStatus())
                .author(author)
                .tags(tags)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Content saved = contentRepository.save(content);
        return mapToResponse(saved);
    }

    public Page<ContentResponse> getAll(Pageable pageable) {
        return contentRepository.findAll(pageable).map(this::mapToResponse);
    }

    public ContentResponse getById(Long id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Контент не найден"));
        return mapToResponse(content);
    }

    @Transactional
    public ContentResponse update(Long id, ContentRequest request, String currentUserEmail, boolean isAdmin) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Контент не найден"));

        if (!content.getAuthor().getEmail().equals(currentUserEmail) && !isAdmin) {
            throw new RuntimeException("У вас нет прав на редактирование этого контента");
        }

        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setType(request.getType());
        content.setBody(request.getBody());
        content.setStatus(request.getStatus());
        content.setTags(processTags(request.getTags()));
        content.setUpdatedAt(Instant.now());

        return mapToResponse(contentRepository.save(content));
    }

    @Transactional
    public void delete(Long id, String currentUserEmail, boolean isAdmin) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Контент не найден"));

        if (!content.getAuthor().getEmail().equals(currentUserEmail) && !isAdmin) {
            throw new RuntimeException("У вас нет прав на удаление этого контента");
        }

        contentRepository.delete(content);
    }

    private Set<Tag> processTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new HashSet<>();

        return tagNames.stream().map(name ->
                tagRepository.findByName(name).orElseGet(() ->
                        tagRepository.save(Tag.builder().name(name).build())
                )
        ).collect(Collectors.toSet());
    }

    private ContentResponse mapToResponse(Content content) {
        return ContentResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .type(content.getType())
                .body(content.getBody())
                .status(content.getStatus())
                .authorEmail(content.getAuthor().getEmail())
                .tags(content.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .build();
    }
}
