package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.ReviewRequest;
import com.web.motoridelab3api.dto.ReviewResponse;
import com.web.motoridelab3api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/motorcycles/{id}/reviews")
@RequiredArgsConstructor
@Tag(name = "Отзывы", description = "Отзывы пользователей о мотоциклах")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(
            summary = "Отзывы о мотоцикле",
            description = "Возвращает все отзывы для указанного мотоцикла, отсортированные по дате создания (сначала новые)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список отзывов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Мотоцикл с таким id не найден")
    })
    public ResponseEntity<List<ReviewResponse>> list(
            @Parameter(description = "ID мотоцикла", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.listByMotorcycle(id));
    }

    @PostMapping
    @Operation(
            summary = "Добавить отзыв",
            description = "Создаёт новый отзыв о мотоцикле. Оценка — целое число от 1 до 5, текст отзыва обязателен."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Отзыв успешно создан",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные отзыва (например, оценка вне диапазона 1–5)"),
            @ApiResponse(responseCode = "404", description = "Мотоцикл с таким id не найден")
    })
    public ResponseEntity<ReviewResponse> add(
            @Parameter(description = "ID мотоцикла", required = true)
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.add(id, request));
    }
}