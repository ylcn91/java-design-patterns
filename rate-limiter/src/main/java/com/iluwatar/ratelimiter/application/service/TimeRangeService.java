package com.iluwatar.ratelimiter.application.service;

import com.iluwatar.ratelimiter.infrastructure.configs.TimeRangeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeRangeService {

  private final TimeRangeConfig timeRangeConfig;

  public String determineTimeRange(LocalTime now) {
    for (Map.Entry<String, TimeRangeConfig.TimeRange> entry : timeRangeConfig.getRanges().entrySet()) {
      TimeRangeConfig.TimeRange range = entry.getValue();
      if (isTimeInRange(now, range.getStart(), range.getEnd())) {
        return entry.getKey();
      }
    }
    return "default";
  }

  private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
    if (start.isBefore(end)) {
      return !time.isBefore(start) && !time.isAfter(end);
    } else {
      return !time.isBefore(start) || !time.isAfter(end);
    }
  }
}
