package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.SocialDtos.CommentRequest;
import com.web.motoridelab3api.dto.SocialDtos.CommentResponse;
import com.web.motoridelab3api.service.PostService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final PostService posts;

    @GetMapping("/{id}")
    public CommentResponse byId(@PathVariable Long id) {
        return posts.commentById(id);
    }

    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable Long id, @Valid @RequestBody CommentRequest r) {
        return posts.updateComment(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        posts.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
