package com.web.motoridelab3api.controller;

import com.web.motoridelab3api.dto.SocialDtos.ReactionRequest;
import com.web.motoridelab3api.dto.SocialDtos.ReactionResponse;
import com.web.motoridelab3api.repository.ReactionRepository.Target;
import com.web.motoridelab3api.service.ReactionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactions;

    @GetMapping("/posts/{id}/reactions")
    public Map<String, Long> postCounts(@PathVariable Long id) {
        return reactions.counts(Target.POST, id);
    }

    @GetMapping("/posts/{id}/reaction")
    public ResponseEntity<ReactionResponse> postOwn(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(reactions.own(Target.POST, id));
    }

    @PutMapping("/posts/{id}/reaction")
    public ResponseEntity<Void> postPut(
            @PathVariable Long id, @Valid @RequestBody ReactionRequest r) {
        reactions.put(Target.POST, id, r.kind());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts/{id}/reaction")
    public ResponseEntity<Void> postDelete(@PathVariable Long id) {
        reactions.delete(Target.POST, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments/{id}/reactions")
    public Map<String, Long> commentCounts(@PathVariable Long id) {
        return reactions.counts(Target.COMMENT, id);
    }

    @GetMapping("/comments/{id}/reaction")
    public ResponseEntity<ReactionResponse> commentOwn(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(reactions.own(Target.COMMENT, id));
    }

    @PutMapping("/comments/{id}/reaction")
    public ResponseEntity<Void> commentPut(
            @PathVariable Long id, @Valid @RequestBody ReactionRequest r) {
        reactions.put(Target.COMMENT, id, r.kind());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{id}/reaction")
    public ResponseEntity<Void> commentDelete(@PathVariable Long id) {
        reactions.delete(Target.COMMENT, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews/{id}/reactions")
    public Map<String, Long> reviewCounts(@PathVariable Long id) {
        return reactions.counts(Target.REVIEW, id);
    }

    @GetMapping("/reviews/{id}/reaction")
    public ResponseEntity<ReactionResponse> reviewOwn(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(reactions.own(Target.REVIEW, id));
    }

    @PutMapping("/reviews/{id}/reaction")
    public ResponseEntity<Void> reviewPut(
            @PathVariable Long id, @Valid @RequestBody ReactionRequest r) {
        reactions.put(Target.REVIEW, id, r.kind());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reviews/{id}/reaction")
    public ResponseEntity<Void> reviewDelete(@PathVariable Long id) {
        reactions.delete(Target.REVIEW, id);
        return ResponseEntity.noContent().build();
    }
}
