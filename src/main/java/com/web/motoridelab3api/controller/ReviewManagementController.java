package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.ReviewRequest;
import com.web.motoridelab3api.dto.ReviewResponse;
import com.web.motoridelab3api.service.ReviewService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewManagementController {
    private final ReviewService reviews;

    @GetMapping("/{id}")
    public ReviewResponse byId(@PathVariable Long id) {
        return reviews.byId(id);
    }

    @PutMapping("/{id}")
    public ReviewResponse update(@PathVariable Long id, @Valid @RequestBody ReviewRequest r) {
        return reviews.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviews.delete(id);
        return ResponseEntity.noContent().build();
    }
}
