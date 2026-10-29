package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.CategoryRequest;
import com.web.motoridelab3api.dto.CategoryResponse;
import com.web.motoridelab3api.exception.ConflictException;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Category;
import com.web.motoridelab3api.repository.CategoryRepository;
import com.web.motoridelab3api.repository.MotorcycleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(
                        c ->
                                new CategoryResponse(
                                        c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse bySlug(String slug) {
        return categoryRepository
                .findBySlug(slug)
                .map(
                        c ->
                                new CategoryResponse(
                                        c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    }

    private final MotorcycleRepository motorcycleRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse create(CategoryRequest r) {
        return save(new Category(), r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest r) {
        return save(require(id), r);
    }

    private Category require(Long id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private CategoryResponse save(Category entity, CategoryRequest r) {
        entity.setName(r.name());
        entity.setSlug(r.slug());
        entity.setDescription(r.description());
        entity = categoryRepository.saveAndFlush(entity);
        return new CategoryResponse(
                entity.getId(), entity.getName(), entity.getSlug(), entity.getDescription());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (motorcycleRepository.existsByCategoryId(id))
            throw new ConflictException("Category is used by motorcycles");
        categoryRepository.delete(entity);
    }
}
