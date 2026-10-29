package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.category"})
    Page<Favorite> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndMotorcycleId(Long userId, Long motorcycleId);

    Optional<Favorite> findByUserIdAndMotorcycleId(Long userId, Long motorcycleId);
}
