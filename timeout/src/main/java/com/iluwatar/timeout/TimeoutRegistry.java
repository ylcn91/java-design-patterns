/*
 * This project is licensed under the MIT license. Module model-view-viewmodel is using ZK framework licensed under LGPL (see lgpl-3.0.txt).
 *
 * The MIT License
 * Copyright © 2014-2022 Ilkka Seppälä
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.iluwatar.timeout;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the {@link TimeoutPolicy} configured for each downstream service.
 *
 * <p>Services that have no explicit policy fall back to a default limit, so callers never have to
 * hard code a duration next to the call site.
 */
public class TimeoutRegistry {

  private final Map<String, TimeoutPolicy> policies = new ConcurrentHashMap<>();
  private final Duration defaultTimeout;

  /**
   * Creates a registry.
   *
   * @param defaultTimeout limit applied to services without an explicit policy
   */
  public TimeoutRegistry(Duration defaultTimeout) {
    this.defaultTimeout = Objects.requireNonNull(defaultTimeout, "defaultTimeout");
  }

  /**
   * Registers or replaces the policy of a service.
   *
   * @param policy the policy to store
   * @return this registry for chaining
   */
  public TimeoutRegistry register(TimeoutPolicy policy) {
    policies.put(policy.serviceName(), policy);
    return this;
  }

  /**
   * Looks up the policy of a service.
   *
   * @param serviceName name of the downstream service
   * @return the registered policy, or one built from the default limit
   */
  public TimeoutPolicy policyFor(String serviceName) {
    return policies.getOrDefault(serviceName, new TimeoutPolicy(serviceName, defaultTimeout));
  }
}
