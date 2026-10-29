package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.FavoriteResponse;
import com.web.motoridelab3api.service.FavoriteService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(favoriteService.list(page, size));
    }

    @PostMapping("/{motorcycleId}")
    public ResponseEntity<FavoriteResponse> add(@PathVariable Long motorcycleId) {
        return ResponseEntity.created(URI.create("/api/favorites/" + motorcycleId))
                .body(favoriteService.add(motorcycleId));
    }

    @DeleteMapping("/{motorcycleId}")
    public ResponseEntity<Void> remove(@PathVariable Long motorcycleId) {
        favoriteService.remove(motorcycleId);
        return ResponseEntity.noContent().build();
    }
}
