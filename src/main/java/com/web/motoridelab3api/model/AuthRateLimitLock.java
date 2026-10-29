package com.web.motoridelab3api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_rate_limit_locks")
@Getter
@NoArgsConstructor
public class AuthRateLimitLock {
    @Id private Integer id;
}
