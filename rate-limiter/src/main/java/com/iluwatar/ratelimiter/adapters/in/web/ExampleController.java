package com.iluwatar.ratelimiter.adapters.in.web;

import com.iluwatar.ratelimiter.application.port.in.RateLimitUseCase;
import com.iluwatar.ratelimiter.domain.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExampleController {

  private final RateLimitUseCase rateLimitUseCase;

  @GetMapping("/api/endpoint1")
  public ResponseEntity<String> endpoint1(@RequestParam String clientId) {
    LOGGER.info("Received request for endpoint1 with clientId: {}", clientId);
    RateLimitResult result = rateLimitUseCase.checkRateLimit("endpoint1", clientId);
    LOGGER.info("RateLimitResult for endpoint1: {}", result);
    if (result.allowed()) {
      LOGGER.info("Request to endpoint1 allowed.");
      return ResponseEntity.ok("Request to endpoint1 processed successfully.");
    } else {
      LOGGER.warn("Rate limit exceeded for endpoint1. Retry after: {} ms.", result.retryAfter().toMillis());
      return ResponseEntity.status(429).body("Rate limit exceeded. Try again after " + result.retryAfter().toMillis() + " ms.");
    }
  }


  @GetMapping("/api/endpoint2")
  public ResponseEntity<String> endpoint2(@RequestParam String clientId) {
    RateLimitResult result = rateLimitUseCase.checkRateLimit("endpoint2", clientId);
    if (result.allowed()) {
      return ResponseEntity.ok("Request to endpoint2 processed successfully.");
    } else {
      return ResponseEntity.status(429).body("Rate limit exceeded. Try again after " + result.retryAfter().toMillis() + " ms.");
    }
  }

  @GetMapping("/api/endpoint3")
  public ResponseEntity<String> endpoint3(@RequestParam String clientId) {
    RateLimitResult result = rateLimitUseCase.checkRateLimit("endpoint3", clientId);
    if (result.allowed()) {
      return ResponseEntity.ok("Request to endpoint3 processed successfully.");
    } else {
      return ResponseEntity.status(429).body("Rate limit exceeded. Try again after " + result.retryAfter().toMillis() + " ms.");
    }
  }

  @GetMapping("/api/endpoint4")
  public ResponseEntity<String> endpoint4(@RequestParam String clientId) {
    RateLimitResult result = rateLimitUseCase.checkRateLimit("endpoint4", clientId);
    if (result.allowed()) {
      return ResponseEntity.ok("Request to endpoint4 processed successfully.");
    } else {
      return ResponseEntity.status(429).body("Rate limit exceeded. Try again after " + result.retryAfter().toMillis() + " ms.");
    }
  }

  @GetMapping("/api/endpoint5")
  public ResponseEntity<String> endpoint5(@RequestParam String clientId) {
    RateLimitResult result = rateLimitUseCase.checkRateLimit("endpoint5", clientId);
    if (result.allowed()) {
      return ResponseEntity.ok("Request to endpoint5 processed successfully.");
    } else {
      return ResponseEntity.status(429).body("Rate limit exceeded. Try again after " + result.retryAfter().toMillis() + " ms.");
    }
  }
}
