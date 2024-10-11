package com.iluwatar.ratelimiter.adapters.out.persistence;

import com.iluwatar.ratelimiter.application.port.out.RateLimitRepository;
import com.iluwatar.ratelimiter.domain.Client;
import com.iluwatar.ratelimiter.domain.ClientType;
import com.iluwatar.ratelimiter.domain.Policy;
import com.iluwatar.ratelimiter.domain.RateLimit;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRateLimitRepository implements RateLimitRepository {

  private final ConcurrentHashMap<String, RateLimit> rateLimits = new ConcurrentHashMap<>();

  @Override
  public RateLimit getRateLimit(String clientId, String endpoint) {
    String key = clientId + ":" + endpoint;
    return rateLimits.computeIfAbsent(key, k ->
        new RateLimit(new Client(clientId, ClientType.STANDARD), new Policy(10), Instant.now(), 0, 0)
    );
  }

  @Override
  public void saveRateLimit(String clientId, String endpoint, RateLimit rateLimit) {
    String key = clientId + ":" + endpoint;
    rateLimits.put(key, rateLimit);
  }
}
