package com.example.mediacmslab.repository;

import com.example.mediacmslab.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Modifying
    @Query(value = "insert into tags (name) values (:name) on conflict (name) do nothing", nativeQuery = true)
    void insertIfAbsent(String name);

    Optional<Tag> findByName(String name);
}
