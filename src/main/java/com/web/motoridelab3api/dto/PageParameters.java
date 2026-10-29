package com.web.motoridelab3api.dto;

import com.web.motoridelab3api.exception.BadRequestException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageParameters {
    private PageParameters() {}

    public static PageRequest of(int page, int size, Sort sort) {
        var validated = of(page, size);
        return PageRequest.of(validated.getPageNumber(), validated.getPageSize(), sort);
    }

    public static PageRequest of(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BadRequestException("page >= 0; size: 1..100");
        return PageRequest.of(page, size);
    }
}
