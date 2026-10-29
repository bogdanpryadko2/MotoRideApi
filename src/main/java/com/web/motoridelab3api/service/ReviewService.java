package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.PageParameters;
import com.web.motoridelab3api.dto.ReviewRequest;
import com.web.motoridelab3api.dto.ReviewResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Motorcycle;
import com.web.motoridelab3api.model.Review;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.repository.ReviewRepository;
import com.web.motoridelab3api.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MotorcycleRepository motorcycleRepository;
    private final CurrentUser current;

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByMotorcycle(Long motorcycleId, int page, int size) {
        if (!motorcycleRepository.existsById(motorcycleId))
            throw new ResourceNotFoundException("Motorcycle not found");
        return reviewRepository
                .findAllByMotorcycleIdOrderByCreatedAtDescIdDesc(
                        motorcycleId, PageParameters.of(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse add(Long motorcycleId, ReviewRequest request) {
        Motorcycle motorcycle =
                motorcycleRepository
                        .findById(motorcycleId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Motorcycle not found: " + motorcycleId));
        Review review = new Review();
        review.setMotorcycle(motorcycle);
        review.setUser(current.get());
        review.setRating(request.rating());
        review.setText(request.text());
        review.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        return toResponse(reviewRepository.save(review));
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getRating(),
                r.getText(),
                r.getCreatedAt(),
                r.getUser() == null ? null : r.getUser().getId(),
                r.getUser() == null ? "Legacy author" : r.getUser().getDisplayName());
    }

    @Transactional(readOnly = true)
    public ReviewResponse byId(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public ReviewResponse update(Long id, ReviewRequest request) {
        var review = require(id);
        authorize(review);
        review.setRating(request.rating());
        review.setText(request.text());
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public void delete(Long id) {
        var review = require(id);
        authorize(review);
        reviewRepository.delete(review);
    }

    private Review require(Long id) {
        return reviewRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    private void authorize(Review review) {
        current.requireOwnerOrAdmin(review.getUser() == null ? null : review.getUser().getId());
    }
}
