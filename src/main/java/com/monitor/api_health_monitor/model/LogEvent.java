package com.monitor.api_health_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String serviceName;
    private String endpoint;
    private String httpMethod;
    private int statusCode;
    private long responseTimeMs;
    private Instant timestamp;
}