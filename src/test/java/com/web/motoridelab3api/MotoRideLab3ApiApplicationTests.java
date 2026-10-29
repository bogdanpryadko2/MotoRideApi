package com.web.motoridelab3api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Category;
import com.web.motoridelab3api.repository.CategoryRepository;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.service.CategoryService;

import org.junit.jupiter.api.Test;

import java.util.List;

class MotoRideLab3ApiApplicationTests {
    @Test
    void preservesCategoryResponseContract() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.findAll()).thenReturn(List.of(new Category(1L, "Sport", "sport", "Track")));
        var result = new CategoryService(repository, mock(MotorcycleRepository.class)).list();
        assertEquals("sport", result.get(0).slug());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void missingCategoryIsNotFound() {
        var service =
                new CategoryService(
                        mock(CategoryRepository.class), mock(MotorcycleRepository.class));
        assertThrows(ResourceNotFoundException.class, () -> service.bySlug("missing"));
    }
}
