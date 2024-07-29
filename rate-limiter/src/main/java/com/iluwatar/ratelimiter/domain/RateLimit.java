package com.iluwatar.ratelimiter.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RateLimit(
    @NotNull Client client,
    @NotNull Policy policy,
    @NotNull Instant lastResetTime,
    @Min(0) int requestCount,
    @Min(0) int quotaUsed) {

  public RateLimit resetRequestCount() {
    return new RateLimit(client, policy, Instant.now(), 0, quotaUsed);
  }

  public RateLimit incrementRequestCount() {
    return new RateLimit(client, policy, lastResetTime, requestCount + 1, quotaUsed);
  }

  public RateLimit incrementQuotaUsed() {
    return new RateLimit(client, policy, lastResetTime, requestCount, quotaUsed + 1);
  }

  public RateLimit resetQuotaUsed() {
    return new RateLimit(client, policy, lastResetTime, requestCount, 0);
  }
}
