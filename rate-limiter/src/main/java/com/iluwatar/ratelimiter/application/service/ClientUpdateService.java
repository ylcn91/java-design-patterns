package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.application.port.in.ClientUpdateUseCase;
import com.iluwatar.ratelimiter.domain.ClientType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientUpdateService implements ClientUpdateUseCase {

  private final ClientIdentificationService clientIdentificationService;

  @Override
  public void updateClientType(String clientId, ClientType newType) {
    clientIdentificationService.updateClientType(clientId, newType);
  }
}