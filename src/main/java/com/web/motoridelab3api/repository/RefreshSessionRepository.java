package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.RefreshSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    @Query("select s.user.id from RefreshSession s where s.tokenHash=:hash")
    Optional<Long> findUserIdByTokenHash(String hash);

    Optional<RefreshSession> findByTokenHash(String hash);

    @Modifying
    @Query(
            "update RefreshSession s set s.revokedAt=:now where s.familyId=:family and s.revokedAt"
                    + " is null")
    int revokeFamily(UUID family, Instant now);

    @Modifying
    @Query(
            "update RefreshSession s set s.revokedAt=:now where s.user.id=:userId and s.revokedAt"
                    + " is null")
    int revokeAll(Long userId, Instant now);
}
