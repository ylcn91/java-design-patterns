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
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Simulated recommendation engine. It is slow, so it demonstrates what happens when a dependency
 * misses its limit: the call is interrupted and the caller continues with a fallback.
 */
@Slf4j
public class RecommendationService {

  /** Name under which the service is registered in the {@link TimeoutRegistry}. */
  public static final String NAME = "recommendations";

  private final Duration latency;

  /**
   * Creates the service.
   *
   * @param latency simulated response time
   */
  public RecommendationService(Duration latency) {
    this.latency = latency;
  }

  /**
   * Computes personalised recommendations for a customer.
   *
   * @param customer customer identifier
   * @return recommended product names
   * @throws InterruptedException if the call is cancelled before the computation finishes
   */
  public List<String> recommendationsFor(String customer) throws InterruptedException {
    LOGGER.info(
        "{}: computing recommendations for {}, expected latency {} ms",
        NAME,
        customer,
        latency.toMillis());
    try {
      Thread.sleep(latency);
    } catch (InterruptedException e) {
      LOGGER.info("{}: interrupted, abandoning the computation for {}", NAME, customer);
      throw e;
    }
    return List.of("Mechanical keyboard", "USB-C dock");
  }
}
