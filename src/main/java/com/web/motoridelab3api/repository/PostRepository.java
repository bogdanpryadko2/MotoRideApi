package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Post;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable page);

    @EntityGraph(attributePaths = {"user"})
    Optional<Post> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from Post entity where entity.id=:id")
    Optional<Post> lockById(Long id);
}
