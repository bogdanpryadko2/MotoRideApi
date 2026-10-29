package com.web.motoridelab3api.service;

import com.web.motoridelab3api.dto.SocialDtos.ReactionResponse;
import com.web.motoridelab3api.exception.ResourceNotFoundException;
import com.web.motoridelab3api.model.Reaction;
import com.web.motoridelab3api.model.ReactionKind;
import com.web.motoridelab3api.repository.PostCommentRepository;
import com.web.motoridelab3api.repository.PostRepository;
import com.web.motoridelab3api.repository.ReactionRepository;
import com.web.motoridelab3api.repository.ReactionRepository.Target;
import com.web.motoridelab3api.repository.ReviewRepository;
import com.web.motoridelab3api.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactions;
    private final PostRepository posts;
    private final PostCommentRepository comments;
    private final ReviewRepository reviews;
    private final CurrentUser current;

    @Transactional(readOnly = true)
    public Map<String, Long> counts(Target target, Long id) {
        require(target, id);
        var result = new LinkedHashMap<String, Long>();
        reactions
                .countByTarget(target.name(), id)
                .forEach(count -> result.put(count.getKind().name(), count.getTotal()));
        return result;
    }

    @Transactional(readOnly = true)
    public ReactionResponse own(Target target, Long id) {
        require(target, id);
        return new ReactionResponse(
                reactions
                        .findOwn(target.name(), id, current.get().getId())
                        .map(Reaction::getKind)
                        .orElse(null));
    }

    @Transactional
    public void put(Target target, Long id, ReactionKind kind) {
        lock(target, id);
        var user = current.get();
        var reaction = reactions.findOwn(target.name(), id, user.getId()).orElseGet(Reaction::new);
        reaction.setUser(user);
        reaction.setKind(kind);
        switch (target) {
            case POST -> reaction.setPost(posts.getReferenceById(id));
            case COMMENT -> reaction.setComment(comments.getReferenceById(id));
            case REVIEW -> reaction.setReview(reviews.getReferenceById(id));
        }
        reactions.saveAndFlush(reaction);
    }

    @Transactional
    public void delete(Target target, Long id) {
        lock(target, id);
        reactions.findOwn(target.name(), id, current.get().getId()).ifPresent(reactions::delete);
    }

    private void lock(Target target, Long id) {
        boolean exists =
                switch (target) {
                    case POST -> posts.lockById(id).isPresent();
                    case COMMENT -> comments.lockById(id).isPresent();
                    case REVIEW -> reviews.lockById(id).isPresent();
                };
        if (!exists) throw new ResourceNotFoundException("Reaction target not found");
    }

    private void require(Target target, Long id) {
        boolean exists =
                switch (target) {
                    case POST -> posts.existsById(id);
                    case COMMENT -> comments.existsById(id);
                    case REVIEW -> reviews.existsById(id);
                };
        if (!exists) throw new ResourceNotFoundException("Reaction target not found");
    }
}
