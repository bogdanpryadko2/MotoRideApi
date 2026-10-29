package com.web.motoridelab3api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limits")
@Getter
@Setter
@NoArgsConstructor
public class AuthRateLimit {
    @Id
    @Column(length = 64)
    private String bucketKey;

    @Column(nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Integer lockId;
}
