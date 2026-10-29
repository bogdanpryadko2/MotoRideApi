package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.Reaction;
import com.web.motoridelab3api.model.ReactionKind;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    enum Target {
        POST,
        COMMENT,
        REVIEW
    }

    interface Count {
        ReactionKind getKind();

        long getTotal();
    }

    @Query(
            """
            select r.kind as kind, count(r) as total from Reaction r
            where (:target = 'POST' and r.post.id = :id)
               or (:target = 'COMMENT' and r.comment.id = :id)
               or (:target = 'REVIEW' and r.review.id = :id)
            group by r.kind order by r.kind
            """)
    List<Count> countByTarget(String target, Long id);

    @Query(
            """
            select r from Reaction r where r.user.id = :userId and (
                (:target = 'POST' and r.post.id = :id)
                or (:target = 'COMMENT' and r.comment.id = :id)
                or (:target = 'REVIEW' and r.review.id = :id))
            """)
    Optional<Reaction> findOwn(String target, Long id, Long userId);
}
