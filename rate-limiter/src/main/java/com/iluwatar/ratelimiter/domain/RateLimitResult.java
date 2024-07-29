package com.iluwatar.ratelimiter.domain;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

public record RateLimitResult(
    boolean allowed,
    @NotNull Duration retryAfter) {
}
