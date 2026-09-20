package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.ReviewRequest;
import com.web.motoridelab3api.dto.ReviewResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Motorcycle;
import com.web.motoridelab3api.model.Review;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MotorcycleRepository motorcycleRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByMotorcycle(Long motorcycleId) {
        return reviewRepository.findAllByMotorcycleIdOrderByCreatedAtDesc(motorcycleId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse add(Long motorcycleId, ReviewRequest request) {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found: " + motorcycleId));
        Review review = new Review();
        review.setMotorcycle(motorcycle);
        review.setRating(request.rating());
        review.setText(request.text());
        review.setCreatedAt(java.time.LocalDateTime.now());
        return toResponse(reviewRepository.save(review));
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getText(), r.getCreatedAt());
    }
}