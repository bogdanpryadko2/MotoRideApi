package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.TechnologyResponse;
import com.web.motoridelab3api.service.TechnologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            description = "Возвращает все технологии из справочника (контроль тяги, квикшифтер, " +
                    "активная подвеска и т.д.), отсортированные по названию."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список технологий",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TechnologyResponse.class))))
    })
    public ResponseEntity<List<TechnologyResponse>> list() {
        return ResponseEntity.ok(technologyService.list());
    }
}