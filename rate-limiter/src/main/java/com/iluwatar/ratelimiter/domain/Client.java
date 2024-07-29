package com.iluwatar.ratelimiter.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Client(
    @NotBlank String id,
    @NotNull ClientType type) {
}
