package com.iluwatar.ratelimiter.adapters.in.web;

import com.iluwatar.ratelimiter.application.service.DynamicConfigService;
import com.iluwatar.ratelimiter.application.service.DynamicConfigService.ConfigUpdateResult;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@Validated
@RequiredArgsConstructor
public class DynamicConfigController {

  private final DynamicConfigService dynamicConfigService;

  @PostMapping("/update-config")
  public ResponseEntity<ConfigUpdateResult> updateConfig(@Valid @RequestBody RateLimitConfig newConfig) {
    ConfigUpdateResult result = dynamicConfigService.updateConfig(newConfig);
    return ResponseEntity.ok(result);
  }
}