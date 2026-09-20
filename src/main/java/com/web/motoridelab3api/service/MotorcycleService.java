package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.CategoryResponse;
import com.web.motoridelab3api.dto.MotorcycleResponse;
import com.web.motoridelab3api.dto.MotorcycleSummaryResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Category;
import com.web.motoridelab3api.model.Motorcycle;
import com.web.motoridelab3api.model.Technology;
import com.web.motoridelab3api.repository.CategoryRepository;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorcycleService {

    private final MotorcycleRepository motorcycleRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<MotorcycleSummaryResponse> list(String categorySlug, String search, String sort) {
        String cat = (categorySlug == null || categorySlug.isBlank()) ? null : categorySlug.trim();
        String q = (search == null || search.isBlank()) ? null : search.trim();

        List<Motorcycle> bikes = motorcycleRepository.search(cat, q);

        Comparator<Motorcycle> cmp = comparator(sort);
        List<Motorcycle> result = cmp != null ? bikes.stream().sorted(cmp).toList() : bikes;

        return result.stream().map(this::toSummary).toList();
    }

    private Comparator<Motorcycle> comparator(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        return switch (sort.toLowerCase(Locale.ROOT)) {
            case "hp_desc", "hp" ->
                    Comparator.comparing(Motorcycle::getHorsepower, Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
            case "price_asc" ->
                    Comparator.comparing(Motorcycle::getPrice, Comparator.nullsFirst(Comparator.naturalOrder()));
            case "price_desc" ->
                    Comparator.comparing(Motorcycle::getPrice, Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
            case "newest" ->
                    Comparator.comparing(Motorcycle::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse byId(Long id) {
        Motorcycle bike = motorcycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found: " + id));
        return toResponse(bike);
    }

    @Transactional(readOnly = true)
    public List<MotorcycleSummaryResponse> top(int limit) {
        return motorcycleRepository.findTop3ByOrderByHorsepowerDesc().stream()
                .limit(limit > 0 ? limit : 3)
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category findCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    }

    public List<MotorcycleSummaryResponse> byCategory(Category category) {
        return motorcycleRepository.findAll(
                        Specification.where((root, query, cb) -> cb.equal(root.get("category"), category)))
                .stream().map(this::toSummary).toList();
    }

    private MotorcycleSummaryResponse toSummary(Motorcycle m) {
        String slug = m.getCategory() != null ? m.getCategory().getSlug() : null;
        String catName = m.getCategory() != null ? m.getCategory().getName() : null;
        return new MotorcycleSummaryResponse(
                m.getId(), m.getBrand(), m.getModel(), m.getEngineCc(), m.getHorsepower(),
                m.getBadge(), m.getImageUrl(), m.getPrice(), slug, catName);
    }

    private MotorcycleResponse toResponse(Motorcycle m) {
        String slug = m.getCategory() != null ? m.getCategory().getSlug() : null;
        String catName = m.getCategory() != null ? m.getCategory().getName() : null;
        List<String> techs = m.getTechnologies() == null ? List.of()
                : m.getTechnologies().stream().map(Technology::getName).toList();
        return new MotorcycleResponse(
                m.getId(), m.getBrand(), m.getModel(), m.getEngineCc(), m.getHorsepower(),
                m.getBadge(), m.getImageUrl(), m.getPrice(), slug, catName,
                m.getShortDescription(), m.getDescription(), techs);
    }
}