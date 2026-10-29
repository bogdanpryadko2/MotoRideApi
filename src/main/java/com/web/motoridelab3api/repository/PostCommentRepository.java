package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.PostComment;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<PostComment> findAllByPostIdOrderByCreatedAtDescIdDesc(Long postId, Pageable page);

    @EntityGraph(attributePaths = {"user"})
    Optional<PostComment> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from PostComment entity where entity.id=:id")
    Optional<PostComment> lockById(Long id);
}
