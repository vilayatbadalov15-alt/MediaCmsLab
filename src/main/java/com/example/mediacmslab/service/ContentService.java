package com.example.mediacmslab.service;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.dto.ContentResponse;
import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.entity.Tag;
import com.example.mediacmslab.repository.ContentRepository;
import com.example.mediacmslab.repository.TagRepository;
import com.example.mediacmslab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    record Created(Long id) {}

    @Transactional
    public ContentResponse create(ContentRequest request, String authorEmail) {
        var author = userRepository.findByEmail(authorEmail).orElseThrow();
        Content content = new Content();
        content.setAuthor(author);
        content.setCreatedAt(Instant.now());
        apply(content, request);
        contentRepository.save(content);
        events.publishEvent(new Created(content.getId()));
        return mapToResponse(content);
    }

    public Page<ContentResponse> getAll(Pageable pageable, String email, boolean admin) {
        var page = admin ? contentRepository.findAll(pageable)
                : contentRepository.findByStatusOrAuthor_Email(Content.Status.PUBLISHED, email, pageable);
        fetchCollections(page.getContent());
        return page.map(ContentService::mapToResponse);
    }

    private void fetchCollections(List<Content> rows) {
        if (!rows.isEmpty()) {
            contentRepository.fetchTags(rows);
            contentRepository.fetchEpisodes(rows);
        }
    }

    public List<ContentResponse> getPublishedInOrder(List<Long> ids) {
        var rows = contentRepository.findByIdInAndStatus(ids, Content.Status.PUBLISHED);
        fetchCollections(rows);
        var byId = rows.stream().collect(Collectors.toMap(Content::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(ContentService::mapToResponse).toList();
    }

    public long revision(Long id) {
        return contentRepository.findRevision(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Cacheable(cacheNames = "content", key = "#id + ':' + #revision")
    public ContentResponse getById(Long id, long revision) {
        return mapToResponse(find(id));
    }

    @Transactional
    public ContentResponse update(Long id, ContentRequest request, String email, boolean admin) {
        Content content = editable(id, email, admin);
        apply(content, request);
        events.publishEvent(new VisibilityChanged(id, content.getStatus() == Content.Status.PUBLISHED, false));
        return mapToResponse(content);
    }

    @Transactional
    public void delete(Long id, String email, boolean admin) {
        contentRepository.delete(editable(id, email, admin));
        events.publishEvent(new VisibilityChanged(id, false, true));
    }

    record VisibilityChanged(Long id, boolean published, boolean deleted) {}

    private Content find(Long id) {
        return contentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Content editable(Long id, String email, boolean admin) {
        Content content = find(id);
        if (!admin && !content.getAuthor().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return content;
    }

    private void apply(Content content, ContentRequest request) {
        content.setUpdatedAt(Instant.now());
        if (request.getStatus() == Content.Status.PUBLISHED && content.getPublishedAt() == null) {
            content.setPublishedAt(content.getStatus() == Content.Status.PUBLISHED
                    ? content.getCreatedAt() : content.getUpdatedAt());
        }
        content.setStatus(request.getStatus());
        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setType(request.getType());
        content.setBody(request.getBody());
        content.setMediaUrl(request.getMediaUrl());
        content.setDurationSeconds(request.getDurationSeconds());
        content.setEpisodes(new ArrayList<>(request.getEpisodes() == null ? List.of() : request.getEpisodes()));
        Set<Tag> tags = new HashSet<>();
        if (request.getTags() != null) {
            for (String name : request.getTags().stream().sorted().toList()) {
                tagRepository.insertIfAbsent(name);
                tags.add(tagRepository.findByName(name).orElseThrow());
            }
        }
        content.setTags(tags);
    }

    static ContentResponse mapToResponse(Content content) {
        var publishedAt = content.getPublishedAt();
        if (publishedAt == null && content.getStatus() == Content.Status.PUBLISHED) publishedAt = content.getCreatedAt();
        return ContentResponse.builder()
                .id(content.getId()).title(content.getTitle()).description(content.getDescription())
                .type(content.getType()).body(content.getBody()).status(content.getStatus())
                .mediaUrl(content.getMediaUrl() == null && content.getType() == Content.ContentType.VIDEO
                        ? content.getBody() : content.getMediaUrl())
                .durationSeconds(content.getDurationSeconds()).episodes(List.copyOf(content.getEpisodes()))
                .authorEmail(content.getAuthor().getEmail()).publishedAt(publishedAt)
                .tags(content.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .createdAt(content.getCreatedAt()).updatedAt(content.getUpdatedAt()).build();
    }
}
