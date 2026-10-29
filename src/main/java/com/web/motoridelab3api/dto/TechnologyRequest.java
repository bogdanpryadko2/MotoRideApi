package com.web.motoridelab3api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TechnologyRequest(
        @NotBlank @Size(max = 100) String name, @Size(max = 10000) String description) {}
