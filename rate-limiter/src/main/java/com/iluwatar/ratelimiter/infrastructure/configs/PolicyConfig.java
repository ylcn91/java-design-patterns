package com.iluwatar.ratelimiter.infrastructure.configs;

import com.iluwatar.ratelimiter.domain.ClientType;
import com.iluwatar.ratelimiter.domain.Policy;
import com.iluwatar.ratelimiter.infrastructure.configs.ratelimit.RateLimitConfig;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PolicyConfig {

  private final RateLimitConfig rateLimitConfig;

  @Bean
  public Map<ClientType, Policy> clientPolicies() {
    Map<ClientType, Policy> policies = new EnumMap<>(ClientType.class);
    policies.put(ClientType.STANDARD, new Policy(rateLimitConfig.getStandardRequestsPerSecond()));
    policies.put(ClientType.PREMIUM, new Policy(rateLimitConfig.getPremiumRequestsPerSecond()));
    policies.put(ClientType.VIP, new Policy(rateLimitConfig.getVipRequestsPerSecond()));
    policies.put(ClientType.ENTERPRISE, new Policy(rateLimitConfig.getEnterpriseRequestsPerSecond()));
    policies.put(ClientType.CUSTOM, new Policy(rateLimitConfig.getCustomRequestsPerSecond()));
    policies.put(ClientType.INTERNAL, Policy.NO_LIMIT);
    return policies;
  }

  public Policy getPolicy(String endpoint, ClientType clientType, String timeRange) {
    int requestsPerSecond;

    // Check for time-based policy first
    if (timeRange != null && rateLimitConfig.getTimeBasedRequestsPerSecond() != null) {
      Map<String, Integer> timeBasedLimits = rateLimitConfig.getTimeBasedRequestsPerSecond().get(timeRange);
      if (timeBasedLimits != null && timeBasedLimits.containsKey(endpoint)) {
        requestsPerSecond = timeBasedLimits.get(endpoint);
        return new Policy(requestsPerSecond);
      }
    }

    // Check for endpoint-specific policy
    if (rateLimitConfig.getEndpointRequestsPerSecond() != null &&
        rateLimitConfig.getEndpointRequestsPerSecond().containsKey(endpoint)) {
      requestsPerSecond = rateLimitConfig.getEndpointRequestsPerSecond().get(endpoint);
    } else {
      // Fall back to client type-based policy
      Policy clientPolicy = clientPolicies().get(clientType);
      if (clientPolicy != null) {
        return clientPolicy;
      }
      throw new IllegalArgumentException("Unsupported client type: " + clientType);
    }

    return new Policy(requestsPerSecond);
  }
}
