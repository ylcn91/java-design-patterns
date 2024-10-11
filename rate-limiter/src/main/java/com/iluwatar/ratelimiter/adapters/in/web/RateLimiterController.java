package com.iluwatar.ratelimiter.adapters.in.web;

import com.iluwatar.ratelimiter.application.port.in.RateLimitUseCase;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RateLimiterController {

  private final RateLimitUseCase rateLimitUseCase;

  @GetMapping("/rate-limit/{endpoint}")
  public ResponseEntity<RateLimitResult> checkRateLimit(@PathVariable String endpoint, @RequestParam String clientId) {
    RateLimitResult result = rateLimitUseCase.checkRateLimit(endpoint, clientId);
    return ResponseEntity.ok(result);
  }
}
