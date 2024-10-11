package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.domain.Client;
import com.iluwatar.ratelimiter.domain.ClientType;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientIdentificationService {

  private final Map<String, Client> clientCache = new ConcurrentHashMap<>();

  @Cacheable("clients")
  public Client identifyClient(String clientId) {
    return clientCache.computeIfAbsent(clientId, this::lookupClient);
  }

  private Client lookupClient(String clientId) {
    // In a real-world scenario, this would involve a database lookup or API call
    // For demonstration, I'll use a simple rule-based approach
    if (clientId.startsWith("premium_")) {
      return new Client(clientId, ClientType.PREMIUM);
    } else if (clientId.startsWith("vip_")) {
      return new Client(clientId, ClientType.VIP);
    } else if (clientId.startsWith("enterprise_")) {
      return new Client(clientId, ClientType.ENTERPRISE);
    } else if (clientId.startsWith("custom_")) {
      return new Client(clientId, ClientType.CUSTOM);
    } else if (clientId.startsWith("internal_")) {
      return new Client(clientId, ClientType.INTERNAL);
    }
    return new Client(clientId, ClientType.STANDARD);
  }

  @CacheEvict(value = "clients", key = "#clientId")
  public void updateClientType(String clientId, ClientType newType) {
    Client updatedClient = new Client(clientId, newType);
    clientCache.put(clientId, updatedClient);
  }

  @Scheduled(fixedRate = 3600000) // Run every hour
  @CacheEvict(value = "clients", allEntries = true)
  public void clearCache() {
    clientCache.clear();
  }
}
