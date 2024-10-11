package com.iluwatar.ratelimiter.adapters.in.web;

import com.iluwatar.ratelimiter.application.port.in.ClientUpdateUseCase;
import com.iluwatar.ratelimiter.domain.ClientType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClientController {
  private final ClientUpdateUseCase clientUpdateUseCase;

  @PostMapping("/client/update-type")
  public ResponseEntity<Void> updateClientType(@RequestParam String clientId, @RequestParam
  ClientType newType) {
    clientUpdateUseCase.updateClientType(clientId, newType);
    return ResponseEntity.ok().build();
  }
}

