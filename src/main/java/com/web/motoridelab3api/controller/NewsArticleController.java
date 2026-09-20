package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.NewsArticleResponse;
import com.web.motoridelab3api.service.NewsArticleService;
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
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "Новости", description = "Публичные новостные статьи о мотоциклах")
public class NewsArticleController {

    private final NewsArticleService newsArticleService;

    @GetMapping
    @Operation(
            summary = "Список новостей",
            description = "Возвращает все новостные статьи с полным содержимым и датой публикации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список новостей",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NewsArticleResponse.class))))
    })
    public ResponseEntity<List<NewsArticleResponse>> list() {
        return ResponseEntity.ok(newsArticleService.list());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Новость по id",
            description = "Возвращает одну новостную статью по её идентификатору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Найденная новость",
                    content = @Content(schema = @Schema(implementation = NewsArticleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Новость с таким id не найдена")
    })
    public ResponseEntity<NewsArticleResponse> byId(
            @Parameter(description = "ID новости", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(newsArticleService.byId(id));
    }
}