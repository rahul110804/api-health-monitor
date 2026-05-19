package com.monitor.api_health_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {
    private String endpoint;
    private String httpMethod;
    private String type;
    private String message;
    private long observedValue;
    private long thresholdValue;
}