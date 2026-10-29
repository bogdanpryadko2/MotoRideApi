package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.NewsArticleRequest;
import com.web.motoridelab3api.dto.NewsArticleResponse;
import com.web.motoridelab3api.dto.PageParameters;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.NewsArticle;
import com.web.motoridelab3api.repository.NewsArticleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> list(int page, int size) {
        return newsArticleRepository
                .findAll(
                        PageParameters.of(
                                page,
                                size,
                                Sort.by("publishedAt")
                                        .descending()
                                        .and(Sort.by("id").descending())))
                .stream()
                .map(
                        n ->
                                new NewsArticleResponse(
                                        n.getId(),
                                        n.getTitle(),
                                        n.getContent(),
                                        n.getImageUrl(),
                                        n.getPublishedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public NewsArticleResponse byId(Long id) {
        return newsArticleRepository
                .findById(id)
                .map(
                        n ->
                                new NewsArticleResponse(
                                        n.getId(),
                                        n.getTitle(),
                                        n.getContent(),
                                        n.getImageUrl(),
                                        n.getPublishedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public NewsArticleResponse create(NewsArticleRequest r) {
        return save(new NewsArticle(), r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public NewsArticleResponse update(Long id, NewsArticleRequest r) {
        return save(require(id), r);
    }

    private NewsArticle require(Long id) {
        return newsArticleRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NewsArticle not found"));
    }

    private NewsArticleResponse save(NewsArticle entity, NewsArticleRequest r) {
        entity.setTitle(r.title());
        entity.setContent(r.content());
        entity.setImageUrl(r.imageUrl());
        entity.setPublishedAt(r.publishedAt());
        entity = newsArticleRepository.saveAndFlush(entity);
        return new NewsArticleResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getImageUrl(),
                entity.getPublishedAt());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        var entity = require(id);

        newsArticleRepository.delete(entity);
    }
}
