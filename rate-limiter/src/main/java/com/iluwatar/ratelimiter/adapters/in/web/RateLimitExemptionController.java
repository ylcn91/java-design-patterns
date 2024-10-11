package com.iluwatar.ratelimiter.adapters.in.web;

import com.iluwatar.ratelimiter.domain.RateLimitExemptionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exemptions")
@RequiredArgsConstructor
public class RateLimitExemptionController {

  private final RateLimitExemptionManager exemptionManager;

  @PostMapping("/add")
  public ResponseEntity<Void> addExemption(@RequestParam String clientId) {
    exemptionManager.addExemption(clientId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/remove")
  public ResponseEntity<Void> removeExemption(@RequestParam String clientId) {
    exemptionManager.removeExemption(clientId);
    return ResponseEntity.ok().build();
  }
}

