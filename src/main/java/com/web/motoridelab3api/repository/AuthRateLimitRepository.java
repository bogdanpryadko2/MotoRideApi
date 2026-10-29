package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.AuthRateLimit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;

public interface AuthRateLimitRepository extends JpaRepository<AuthRateLimit, String> {
    @Modifying
    @Query(
            "delete from AuthRateLimit bucket where bucket.lockId in :lockIds and"
                    + " bucket.windowStart < :cutoff")
    int deleteExpired(Collection<Integer> lockIds, Instant cutoff);
}
