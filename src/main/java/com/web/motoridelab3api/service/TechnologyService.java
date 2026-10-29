package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.TechnologyRequest;
import com.web.motoridelab3api.dto.TechnologyResponse;
import com.web.motoridelab3api.exception.ConflictException;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Technology;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.repository.TechnologyRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<TechnologyResponse> list() {
        return technologyRepository.findAllByOrderByNameAsc().stream()
                .map(t -> new TechnologyResponse(t.getId(), t.getName(), t.getDescription()))
                .toList();
    }

    private final MotorcycleRepository motorcycleRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TechnologyResponse create(TechnologyRequest r) {
        return save(new Technology(), r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TechnologyResponse update(Long id, TechnologyRequest r) {
        return save(require(id), r);
    }

    private Technology require(Long id) {
        return technologyRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology not found"));
    }

    private TechnologyResponse save(Technology entity, TechnologyRequest r) {
        entity.setName(r.name());
        entity.setDescription(r.description());
        entity = technologyRepository.saveAndFlush(entity);
        return new TechnologyResponse(entity.getId(), entity.getName(), entity.getDescription());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (motorcycleRepository.existsByTechnologiesId(id))
            throw new ConflictException("Technology is used by motorcycles");
        technologyRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public TechnologyResponse byId(Long id) {
        var entity = require(id);
        return new TechnologyResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
