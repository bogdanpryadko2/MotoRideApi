package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.TechnologyRequest;
import com.web.motoridelab3api.dto.TechnologyResponse;
import com.web.motoridelab3api.service.TechnologyService;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/technologies")
@RequiredArgsConstructor
@Tag(name = "Технологии", description = "Справочник технологий, применяемых в мотоциклах")
public class TechnologyController {

    private final TechnologyService technologyService;

    @GetMapping
    @Operation(
            summary = "Список технологий",
            description =
                    "Возвращает все технологии из справочника (контроль тяги, квикшифтер, "
                            + "активная подвеска и т.д.), отсортированные по названию.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Список технологий",
                content =
                        @Content(
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        TechnologyResponse.class))))
    })
    public ResponseEntity<List<TechnologyResponse>> list() {
        return ResponseEntity.ok(technologyService.list());
    }

    @PostMapping
    @Operation(summary = "ADMIN: создать ресурс")
    public ResponseEntity<TechnologyResponse> create(
            @Valid @RequestBody TechnologyRequest request) {
        var result = technologyService.create(request);
        return ResponseEntity.created(URI.create("/api/technologies/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "ADMIN: полностью обновить ресурс")
    public ResponseEntity<TechnologyResponse> update(
            @PathVariable Long id, @Valid @RequestBody TechnologyRequest request) {
        return ResponseEntity.ok(technologyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "ADMIN: удалить ресурс")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        technologyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TechnologyResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(technologyService.byId(id));
    }
}
