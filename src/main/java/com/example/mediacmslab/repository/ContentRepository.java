package com.example.mediacmslab.repository;

import com.example.mediacmslab.entity.Content;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from Content c where c.id = :id and c.status = :status")
    Optional<Content> findPublishedForView(Long id, Content.Status status);

    Page<Content> findByStatusOrAuthor_Email(Content.Status status, String email, Pageable pageable);
}
