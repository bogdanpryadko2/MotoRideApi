package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByMotorcycleIdOrderByCreatedAtDesc(Long motorcycleId);
}