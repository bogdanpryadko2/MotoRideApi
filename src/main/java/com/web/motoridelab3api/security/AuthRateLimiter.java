package com.web.motoridelab3api.security;

import com.web.motoridelab3api.exception.TooManyRequestsException;
import com.web.motoridelab3api.model.AuthRateLimit;
import com.web.motoridelab3api.repository.AuthRateLimitLockRepository;
import com.web.motoridelab3api.repository.AuthRateLimitRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration RETENTION = Duration.ofDays(1);
    private static final int LOCK_COUNT = 64;
    private final AuthRateLimitRepository buckets;
    private final AuthRateLimitLockRepository locks;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = TooManyRequestsException.class)
    public void check(String operation, String ip, String email) {
        String ipKey = TokenHasher.hash(operation + ":ip:" + ip);
        String emailKey =
                TokenHasher.hash(operation + ":email:" + email.strip().toLowerCase(Locale.ROOT));
        var lockIds =
                List.of(lockId(ipKey), lockId(emailKey)).stream().distinct().sorted().toList();
        if (locks.lockByIds(lockIds).size() != lockIds.size())
            throw new IllegalStateException("Rate limit locks are missing");
        Instant now = Instant.now();
        buckets.deleteExpired(lockIds, now.minus(RETENTION));
        consume(ipKey, 30, now);
        consume(emailKey, 10, now);
    }

    private void consume(String key, int limit, Instant now) {
        var bucket =
                buckets.findById(key)
                        .orElseGet(
                                () -> {
                                    var created = new AuthRateLimit();
                                    created.setBucketKey(key);
                                    created.setLockId(lockId(key));
                                    created.setWindowStart(now);
                                    return created;
                                });
        if (bucket.getWindowStart().isBefore(now.minus(WINDOW))) {
            bucket.setWindowStart(now);
            bucket.setAttempts(0);
        }
        bucket.setAttempts(bucket.getAttempts() + 1);
        buckets.save(bucket);
        if (bucket.getAttempts() > limit) throw new TooManyRequestsException();
    }

    private int lockId(String key) {
        return Integer.parseInt(key.substring(0, 2), 16) % LOCK_COUNT;
    }
}
