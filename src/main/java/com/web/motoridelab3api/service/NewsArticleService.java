package com.web.motoridelab3api.service;
import com.web.motoridelab3api.dto.NewsArticleResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;

    @Transactional(readOnly = true)
    public List<NewsArticleResponse> list() {
        return newsArticleRepository.findAll().stream()
                .map(n -> new NewsArticleResponse(n.getId(), n.getTitle(), n.getContent(), n.getImageUrl(), n.getPublishedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public NewsArticleResponse byId(Long id) {
        return newsArticleRepository.findById(id)
                .map(n -> new NewsArticleResponse(n.getId(), n.getTitle(), n.getContent(), n.getImageUrl(), n.getPublishedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + id));
    }
}