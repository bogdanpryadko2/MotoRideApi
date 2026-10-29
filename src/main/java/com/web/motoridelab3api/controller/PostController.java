package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.SocialDtos.CommentRequest;
import com.web.motoridelab3api.dto.SocialDtos.CommentResponse;
import com.web.motoridelab3api.dto.SocialDtos.PageResponse;
import com.web.motoridelab3api.dto.SocialDtos.PostRequest;
import com.web.motoridelab3api.dto.SocialDtos.PostResponse;
import com.web.motoridelab3api.service.PostService;

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

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService posts;

    @GetMapping
    public PageResponse<PostResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return posts.list(page, size);
    }

    @GetMapping("/{id}")
    public PostResponse byId(@PathVariable Long id) {
        return posts.byId(id);
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest r) {
        var post = posts.create(r);
        return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest r) {
        return posts.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        posts.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public PageResponse<CommentResponse> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return posts.comments(id, page, size);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> comment(
            @PathVariable Long id, @Valid @RequestBody CommentRequest r) {
        var comment = posts.addComment(id, r);
        return ResponseEntity.created(URI.create("/api/comments/" + comment.id())).body(comment);
    }
}
