package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findAllByOrderByCreatedAtDesc();
    boolean existsByMotorcycleId(Long motorcycleId);
    Optional<Favorite> findByMotorcycleId(Long motorcycleId);
    void deleteByMotorcycleId(Long motorcycleId);
}
