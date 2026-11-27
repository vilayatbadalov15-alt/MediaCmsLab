package com.example.mediacmslab.repository;

import com.example.mediacmslab.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    boolean existsByIdAndStatus(Long id, Content.Status status);

    @Query("select c.revision from Content c where c.id = :id")
    Optional<Long> findRevision(Long id);

    @EntityGraph(attributePaths = "author")
    List<Content> findByIdInAndStatus(List<Long> ids, Content.Status status);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Content> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Content> findByStatusOrAuthor_Email(Content.Status status, String email, Pageable pageable);

    @Query("select c from Content c left join fetch c.tags where c in :page")
    List<Content> fetchTags(List<Content> page);

    @Query("select c from Content c left join fetch c.episodes where c in :page")
    List<Content> fetchEpisodes(List<Content> page);
}
