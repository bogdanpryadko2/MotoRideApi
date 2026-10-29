package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.NewsArticleRequest;
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

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
            description = "Возвращает все новостные статьи с полным содержимым и датой публикации.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Список новостей",
                content =
                        @Content(
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        NewsArticleResponse
                                                                                .class))))
    })
    public ResponseEntity<List<NewsArticleResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(newsArticleService.list(page, size));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Новость по id",
            description = "Возвращает одну новостную статью по её идентификатору.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Найденная новость",
                content = @Content(schema = @Schema(implementation = NewsArticleResponse.class))),
        @ApiResponse(responseCode = "404", description = "Новость с таким id не найдена")
    })
    public ResponseEntity<NewsArticleResponse> byId(
            @Parameter(description = "ID новости", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(newsArticleService.byId(id));
    }

    @PostMapping
    @Operation(summary = "ADMIN: создать ресурс")
    public ResponseEntity<NewsArticleResponse> create(
            @Valid @RequestBody NewsArticleRequest request) {
        var result = newsArticleService.create(request);
        return ResponseEntity.created(URI.create("/api/news/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "ADMIN: полностью обновить ресурс")
    public ResponseEntity<NewsArticleResponse> update(
            @PathVariable Long id, @Valid @RequestBody NewsArticleRequest request) {
        return ResponseEntity.ok(newsArticleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "ADMIN: удалить ресурс")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsArticleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
