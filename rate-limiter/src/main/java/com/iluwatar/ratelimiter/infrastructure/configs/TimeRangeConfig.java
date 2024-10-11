package com.iluwatar.ratelimiter.infrastructure.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ratelimiter.timerange")
public class TimeRangeConfig {
  private Map<String, TimeRange> ranges = new TreeMap<>();

  @Getter
  @Setter
  public static class TimeRange {
    private LocalTime start;
    private LocalTime end;
  }
}