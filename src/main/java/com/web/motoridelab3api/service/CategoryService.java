package com.web.motoridelab3api.service;
import com.web.motoridelab3api.dto.CategoryResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
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
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse bySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription()))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    }
}