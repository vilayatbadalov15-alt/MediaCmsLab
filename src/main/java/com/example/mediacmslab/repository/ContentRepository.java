package com.example.mediacmslab.repository;

import com.example.mediacmslab.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {
}
