package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Review;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = "user")
    Page<Review> findAllByMotorcycleIdOrderByCreatedAtDescIdDesc(Long motorcycleId, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from Review entity where entity.id=:id")
    Optional<Review> lockById(Long id);
}
