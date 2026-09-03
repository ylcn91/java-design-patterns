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
 * The Timeout pattern bounds how long a caller waits for a downstream service. Without a limit a
 * single slow dependency can hold threads, connections and user requests hostage until the whole
 * system stalls. With a limit the caller abandons the slow call, records the event and continues
 * with a fallback, keeping latency predictable and failures contained.
 *
 * <p>The building blocks are a {@link TimeoutPolicy} per service, a {@link TimeoutRegistry} that
 * makes the limits configurable in one place, and a {@link TimeoutExecutor} that enforces them,
 * cancels calls that overrun, and counts timeouts in {@link TimeoutMetrics}.
 *
 * <p>The demo wires two services with different limits. The product catalog answers well within its
 * 500 ms budget and returns real data. The recommendation engine needs 400 ms but is only allowed
 * 100 ms, so its call is cancelled and the customer sees popular items instead. The timeout
 * counters are printed at the end.
 */
@Slf4j
public class App {

  private static final List<String> POPULAR_ITEMS = List.of("Wireless mouse", "Webcam");

  /**
   * Program entry point.
   *
   * @param args command line arguments, not used
   */
  public static void main(String[] args) {
    var registry =
        new TimeoutRegistry(Duration.ofMillis(300))
            .register(TimeoutPolicy.of(ProductCatalogService.NAME, 500))
            .register(TimeoutPolicy.of(RecommendationService.NAME, 100));
    LOGGER.info("Configured per-service limits: catalog 500 ms, recommendations 100 ms");

    var catalog = new ProductCatalogService(Duration.ofMillis(50));
    var recommendations = new RecommendationService(Duration.ofMillis(400));

    try (var executor = new TimeoutExecutor()) {
      LOGGER.info("Calling {}", ProductCatalogService.NAME);
      var products = loadProducts(executor, registry, catalog);
      LOGGER.info("Products: {}", products);

      LOGGER.info("Calling {}", RecommendationService.NAME);
      var suggested = loadRecommendations(executor, registry, recommendations, "alice");
      LOGGER.info("Recommendations shown to alice: {}", suggested);

      LOGGER.info("Timeouts per service: {}", executor.metrics().snapshot());
    }
  }

  /**
   * Loads the catalog under its time limit, showing an empty catalog if the limit is exceeded.
   *
   * @param executor executor enforcing the limit
   * @param registry registry holding the catalog's policy
   * @param catalog the downstream catalog service
   * @return the products, or an empty list on timeout
   */
  static List<String> loadProducts(
      TimeoutExecutor executor, TimeoutRegistry registry, ProductCatalogService catalog) {
    return executor.execute(
        registry.policyFor(ProductCatalogService.NAME), catalog::fetchProducts, List::of);
  }

  /**
   * Loads personalised recommendations under their time limit, showing popular items instead if the
   * limit is exceeded.
   *
   * @param executor executor enforcing the limit
   * @param registry registry holding the recommendation service's policy
   * @param recommendations the downstream recommendation service
   * @param customer customer to personalise for
   * @return the recommendations, or the popular items on timeout
   */
  static List<String> loadRecommendations(
      TimeoutExecutor executor,
      TimeoutRegistry registry,
      RecommendationService recommendations,
      String customer) {
    return executor.execute(
        registry.policyFor(RecommendationService.NAME),
        () -> recommendations.recommendationsFor(customer),
        () -> POPULAR_ITEMS);
  }
}
