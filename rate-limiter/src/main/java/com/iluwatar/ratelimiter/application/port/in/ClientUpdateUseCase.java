package com.iluwatar.ratelimiter.application.port.in;

import com.iluwatar.ratelimiter.domain.ClientType;

public interface ClientUpdateUseCase {
  void updateClientType(String clientId, ClientType newType);
}