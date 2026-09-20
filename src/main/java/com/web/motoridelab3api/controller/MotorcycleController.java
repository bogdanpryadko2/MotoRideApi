package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.MotorcycleResponse;
import com.web.motoridelab3api.dto.MotorcycleSummaryResponse;
import com.web.motoridelab3api.service.MotorcycleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/motorcycles")
@RequiredArgsConstructor
@Tag(name = "Мотоциклы", description = "Публичный каталог мотоциклов с фильтрацией, поиском и сортировкой")
public class MotorcycleController {

    private final MotorcycleService motorcycleService;

    @GetMapping
    @Operation(
            summary = "Каталог мотоциклов",
            description = "Возвращает список мотоциклов с возможностью фильтрации по категории, " +
                    "поиска по бренду/модели и сортировки. Все параметры необязательны."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список мотоциклов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MotorcycleSummaryResponse.class))))
    })
    public ResponseEntity<List<MotorcycleSummaryResponse>> list(
            @Parameter(description = "Slug категории для фильтрации, например 'sport'", required = false)
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Поисковая строка по бренду или модели", required = false)
            @RequestParam(value = "search", required = false) String search,
            @Parameter(
                    description = "Поле сортировки. Допустимые значения: 'hp_desc' (по мощности), " +
                            "'price_asc', 'price_desc', 'newest' (по дате добавления)",
                    required = false)
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(motorcycleService.list(category, search, sort));
    }

    @GetMapping("/top")
    @Operation(
            summary = "Топ-3 самых мощных мотоцикла",
            description = "Возвращает три мотоцикла с наибольшей мощностью в порядке убывания лошадиных сил."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список топ-3 мотоциклов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MotorcycleSummaryResponse.class))))
    })
    public ResponseEntity<List<MotorcycleSummaryResponse>> top() {
        return ResponseEntity.ok(motorcycleService.top(3));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Мотоцикл по id",
            description = "Возвращает подробную информацию о мотоцикле, включая описание, " +
                    "категорию и список технологий."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Найденный мотоцикл",
                    content = @Content(schema = @Schema(implementation = MotorcycleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Мотоцикл с таким id не найден")
    })
    public ResponseEntity<MotorcycleResponse> byId(
            @Parameter(description = "ID мотоцикла", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(motorcycleService.byId(id));
    }
}