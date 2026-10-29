package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.PageParameters;
import com.web.motoridelab3api.dto.SocialDtos.Author;
import com.web.motoridelab3api.dto.SocialDtos.CommentRequest;
import com.web.motoridelab3api.dto.SocialDtos.CommentResponse;
import com.web.motoridelab3api.dto.SocialDtos.PageResponse;
import com.web.motoridelab3api.dto.SocialDtos.PostRequest;
import com.web.motoridelab3api.dto.SocialDtos.PostResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Post;
import com.web.motoridelab3api.model.PostComment;
import com.web.motoridelab3api.repository.MotorcycleRepository;
import com.web.motoridelab3api.repository.PostCommentRepository;
import com.web.motoridelab3api.repository.PostRepository;
import com.web.motoridelab3api.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository posts;
    private final PostCommentRepository comments;
    private final MotorcycleRepository motorcycles;
    private final CurrentUser current;

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(int page, int size) {
        return PageResponse.from(
                posts.findAllByOrderByCreatedAtDescIdDesc(PageParameters.of(page, size))
                        .map(this::response));
    }

    @Transactional(readOnly = true)
    public PostResponse byId(Long id) {
        return response(post(id));
    }

    @Transactional
    public PostResponse create(PostRequest r) {
        var post = new Post();
        post.setUser(current.get());
        return save(post, r);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest r) {
        var post = post(id);
        current.requireOwnerOrAdmin(post.getUser().getId());
        return save(post, r);
    }

    @Transactional
    public void delete(Long id) {
        var post = post(id);
        current.requireOwnerOrAdmin(post.getUser().getId());
        posts.delete(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> comments(Long postId, int page, int size) {
        requirePost(postId);
        return PageResponse.from(
                comments.findAllByPostIdOrderByCreatedAtDescIdDesc(
                                postId, PageParameters.of(page, size))
                        .map(this::response));
    }

    @Transactional(readOnly = true)
    public CommentResponse commentById(Long id) {
        return response(comment(id));
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest r) {
        var comment = new PostComment();
        comment.setPost(post(postId));
        comment.setUser(current.get());
        comment.setText(r.text());
        return response(comments.saveAndFlush(comment));
    }

    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest r) {
        var comment = comment(id);
        current.requireOwnerOrAdmin(comment.getUser().getId());
        comment.setText(r.text());
        return response(comments.saveAndFlush(comment));
    }

    @Transactional
    public void deleteComment(Long id) {
        var comment = comment(id);
        current.requireOwnerOrAdmin(comment.getUser().getId());
        comments.delete(comment);
    }

    private PostResponse save(Post post, PostRequest r) {
        post.setTitle(r.title());
        post.setContent(r.content());
        post.setImageUrl(r.imageUrl());
        post.setMotorcycle(
                r.motorcycleId() == null
                        ? null
                        : motorcycles
                                .findById(r.motorcycleId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Motorcycle not found")));
        return response(posts.saveAndFlush(post));
    }

    private void requirePost(Long id) {
        if (!posts.existsById(id)) throw new ResourceNotFoundException("Post not found");
    }

    private Post post(Long id) {
        return posts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private PostComment comment(Long id) {
        return comments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private PostResponse response(Post p) {
        return new PostResponse(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getImageUrl(),
                p.getMotorcycle() == null ? null : p.getMotorcycle().getId(),
                new Author(p.getUser().getId(), p.getUser().getDisplayName()),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private CommentResponse response(PostComment c) {
        return new CommentResponse(
                c.getId(),
                c.getPost().getId(),
                c.getText(),
                new Author(c.getUser().getId(), c.getUser().getDisplayName()),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
