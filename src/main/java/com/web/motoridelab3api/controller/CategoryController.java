package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.CategoryResponse;
import com.web.motoridelab3api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Категории", description = "Публичный список категорий мотоциклов")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "Список всех категорий",
            description = "Возвращает все категории мотоциклов (Спорт, Нейкед, Туризм, Эндуро и т.д.)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список категорий",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    })
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.list());
    }

    @GetMapping("/{slug}")
    @Operation(
            summary = "Категория по slug",
            description = "Возвращает одну категорию по её человекочитаемому идентификатору, например 'sport' или 'naked'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Найденная категория",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория с таким slug не найдена")
    })
    public ResponseEntity<CategoryResponse> bySlug(
            @Parameter(description = "Slug категории, например 'sport'", required = true)
            @PathVariable String slug) {
        return ResponseEntity.ok(categoryService.bySlug(slug));
    }
}