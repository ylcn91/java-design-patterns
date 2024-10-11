package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.domain.Client;
import com.iluwatar.ratelimiter.domain.RateLimitExemption;
import com.iluwatar.ratelimiter.domain.RateLimitExemptionManager;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitExemptionService implements RateLimitExemption, RateLimitExemptionManager {
  private final Set<String> exemptClients = ConcurrentHashMap.newKeySet();

  @Override
  public boolean isExempt(Client client) {
    return exemptClients.contains(client.id());
  }

  @Override
  public void addExemption(String clientId) {
    exemptClients.add(clientId);
  }

  @Override
  public void removeExemption(String clientId) {
    exemptClients.remove(clientId);
  }
}