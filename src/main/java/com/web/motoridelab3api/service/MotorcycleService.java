package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.MotorcycleRequest;
import com.web.motoridelab3api.dto.MotorcycleResponse;
import com.web.motoridelab3api.dto.MotorcycleSummaryResponse;
import com.web.motoridelab3api.exception.BadRequestException;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Category;
import com.web.motoridelab3api.model.Motorcycle;
import com.web.motoridelab3api.model.Technology;
import com.web.motoridelab3api.repository.CategoryRepository;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.repository.TechnologyRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MotorcycleService {

    private final MotorcycleRepository motorcycleRepository;
    private final CategoryRepository categoryRepository;

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<MotorcycleSummaryResponse> list(String category, String search, String sort) {
        return page(category, search, sort, 0, 20).getContent();
    }

    @Transactional(readOnly = true)
    public Page<MotorcycleSummaryResponse> page(
            String category, String search, String sort, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BadRequestException("page >= 0; size: 1..100");
        var order =
                switch (sort == null ? "newest" : sort) {
                    case "hp_desc", "hp" -> Sort.by("horsepower").descending();
                    case "price_asc" -> Sort.by("price").ascending();
                    case "price_desc" -> Sort.by("price").descending();
                    case "newest", "" -> Sort.by("createdAt").descending();
                    default -> throw new BadRequestException("Unknown sort");
                };
        Specification<Motorcycle> filter =
                (root, query, cb) -> {
                    var predicates = new ArrayList<Predicate>();
                    if (category != null && !category.isBlank())
                        predicates.add(cb.equal(root.get("category").get("slug"), category.trim()));
                    if (search != null && !search.isBlank()) {
                        String pattern =
                                "%"
                                        + search.trim()
                                                .toLowerCase(Locale.ROOT)
                                                .replace("\\", "\\\\")
                                                .replace("%", "\\%")
                                                .replace("_", "\\_")
                                        + "%";
                        predicates.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("brand")), pattern, '\\'),
                                        cb.like(cb.lower(root.get("model")), pattern, '\\')));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                };
        return motorcycleRepository
                .findAll(filter, PageRequest.of(page, size, order.and(Sort.by("id"))))
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public void requireExists(Long id) {
        if (!motorcycleRepository.existsById(id))
            throw new ResourceNotFoundException("Motorcycle not found: " + id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MotorcycleResponse create(MotorcycleRequest request) {
        return save(new Motorcycle(), request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MotorcycleResponse update(Long id, MotorcycleRequest request) {
        return save(
                motorcycleRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found")),
                request);
    }

    private MotorcycleResponse save(Motorcycle bike, MotorcycleRequest r) {
        bike.setCategory(
                categoryRepository
                        .findById(r.categoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
        var technologies = technologyRepository.findAllById(r.technologyIds());
        if (technologies.size() != r.technologyIds().size())
            throw new ResourceNotFoundException("Technology not found");
        bike.setTechnologies(new ArrayList<>(technologies));
        bike.setBrand(r.brand());
        bike.setModel(r.model());
        bike.setEngineCc(r.engineCc());
        bike.setHorsepower(r.horsepower());
        bike.setBadge(r.badge());
        bike.setShortDescription(r.shortDescription());
        bike.setDescription(r.description());
        bike.setImageUrl(r.imageUrl());
        bike.setPrice(r.price());
        return toResponse(motorcycleRepository.save(bike));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        var bike =
                motorcycleRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found"));
        motorcycleRepository.delete(bike);
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse byId(Long id) {
        Motorcycle bike =
                motorcycleRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Motorcycle not found: " + id));
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
        return categoryRepository
                .findBySlug(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    }

    public List<MotorcycleSummaryResponse> byCategory(Category category) {
        return motorcycleRepository
                .findAll(
                        Specification.where(
                                (root, query, cb) -> cb.equal(root.get("category"), category)))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private MotorcycleSummaryResponse toSummary(Motorcycle m) {
        String slug = m.getCategory() != null ? m.getCategory().getSlug() : null;
        String catName = m.getCategory() != null ? m.getCategory().getName() : null;
        return new MotorcycleSummaryResponse(
                m.getId(),
                m.getBrand(),
                m.getModel(),
                m.getEngineCc(),
                m.getHorsepower(),
                m.getBadge(),
                m.getImageUrl(),
                m.getPrice(),
                slug,
                catName);
    }

    private MotorcycleResponse toResponse(Motorcycle m) {
        String slug = m.getCategory() != null ? m.getCategory().getSlug() : null;
        String catName = m.getCategory() != null ? m.getCategory().getName() : null;
        List<String> techs =
                m.getTechnologies() == null
                        ? List.of()
                        : m.getTechnologies().stream().map(Technology::getName).toList();
        return new MotorcycleResponse(
                m.getId(),
                m.getBrand(),
                m.getModel(),
                m.getEngineCc(),
                m.getHorsepower(),
                m.getBadge(),
                m.getImageUrl(),
                m.getPrice(),
                slug,
                catName,
                m.getShortDescription(),
                m.getDescription(),
                techs,
                m.getCategory() == null ? null : m.getCategory().getId(),
                m.getTechnologies().stream().map(Technology::getId).toList());
    }
}
