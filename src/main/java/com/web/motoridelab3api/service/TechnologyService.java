package com.web.motoridelab3api.service;
import com.web.motoridelab3api.dto.TechnologyResponse;
import com.web.motoridelab3api.repository.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<TechnologyResponse> list() {
        return technologyRepository.findAll().stream()
                .map(t -> new TechnologyResponse(t.getId(), t.getName(), t.getDescription()))
                .toList();
    }
}