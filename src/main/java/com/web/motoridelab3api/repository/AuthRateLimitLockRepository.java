package com.web.motoridelab3api.repository;

import com.web.motoridelab3api.model.AuthRateLimitLock;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AuthRateLimitLockRepository extends JpaRepository<AuthRateLimitLock, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stripe from AuthRateLimitLock stripe where stripe.id in :ids order by stripe.id")
    List<AuthRateLimitLock> lockByIds(Collection<Integer> ids);
}
