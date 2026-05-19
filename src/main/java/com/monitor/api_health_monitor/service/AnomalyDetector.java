package com.monitor.api_health_monitor.service;

import com.monitor.api_health_monitor.model.AnomalyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetector {

    private final AlertService alertService;

    private static final long P95_LATENCY_THRESHOLD_MS = 1000;
    private static final double ERROR_RATE_THRESHOLD_PCT = 10.0;

    public void check(String endpoint,
                      String httpMethod,
                      long p95Latency,
                      double errorRate) {

        boolean isLatencyAnomaly = p95Latency > P95_LATENCY_THRESHOLD_MS;
        boolean isErrorRateAnomaly = errorRate > ERROR_RATE_THRESHOLD_PCT;

        if (isLatencyAnomaly) {
            AnomalyEvent anomaly = AnomalyEvent.builder()
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .type("HIGH_LATENCY")
                    .message(String.format(
                            "p95 latency is %dms — exceeds threshold of %dms",
                            p95Latency, P95_LATENCY_THRESHOLD_MS))
                    .observedValue(p95Latency)
                    .thresholdValue(P95_LATENCY_THRESHOLD_MS)
                    .build();

            log.warn("ANOMALY DETECTED: {}", anomaly);
            alertService.handleAnomaly(anomaly);
        }

        if (isErrorRateAnomaly) {
            AnomalyEvent anomaly = AnomalyEvent.builder()
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .type("HIGH_ERROR_RATE")
                    .message(String.format(
                            "Error rate is %.1f%% — exceeds threshold of %.1f%%",
                            errorRate, ERROR_RATE_THRESHOLD_PCT))
                    .observedValue((long) errorRate)
                    .thresholdValue((long) ERROR_RATE_THRESHOLD_PCT)
                    .build();

            log.warn("ANOMALY DETECTED: {}", anomaly);
            alertService.handleAnomaly(anomaly);
        }

        if (!isLatencyAnomaly && !isErrorRateAnomaly) {
            log.info("Endpoint [{} {}] is healthy.", httpMethod, endpoint);
        }
    }
}