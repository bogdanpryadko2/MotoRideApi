package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.FavoriteResponse;
import com.web.motoridelab3api.dto.MotorcycleSummaryResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.exception.BadRequestException;
import com.web.motoridelab3api.model.Favorite;
import com.web.motoridelab3api.model.Motorcycle;
import com.web.motoridelab3api.repository.FavoriteRepository;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MotorcycleRepository motorcycleRepository;

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list() {
        return favoriteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FavoriteResponse add(Long motorcycleId) {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found: " + motorcycleId));
        if (favoriteRepository.existsByMotorcycleId(motorcycleId)) {
            throw new BadRequestException("Motorcycle already in favorites");
        }
        Favorite favorite = new Favorite();
        favorite.setMotorcycle(motorcycle);
        favorite.setCreatedAt(LocalDateTime.now());
        return toResponse(favoriteRepository.save(favorite));
    }

    @Transactional
    public void remove(Long motorcycleId) {
        Favorite favorite = favoriteRepository.findByMotorcycleId(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found: " + motorcycleId));
        favoriteRepository.delete(favorite);
    }

    private FavoriteResponse toResponse(Favorite favorite) {
        Motorcycle m = favorite.getMotorcycle();
        String slug = m.getCategory() != null ? m.getCategory().getSlug() : null;
        String catName = m.getCategory() != null ? m.getCategory().getName() : null;
        MotorcycleSummaryResponse summary = new MotorcycleSummaryResponse(
                m.getId(), m.getBrand(), m.getModel(), m.getEngineCc(), m.getHorsepower(),
                m.getBadge(), m.getImageUrl(), m.getPrice(), slug, catName);
        return new FavoriteResponse(favorite.getId(), summary, favorite.getCreatedAt());
    }
}