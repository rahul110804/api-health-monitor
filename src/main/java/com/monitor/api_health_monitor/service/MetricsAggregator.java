package com.monitor.api_health_monitor.service;

import com.monitor.api_health_monitor.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsAggregator {

    private final InfluxDBWriter influxDBWriter;
    private final AnomalyDetector anomalyDetector;

    // stores raw log events per endpoint since last flush
    private final Map<String, List<LogEvent>> eventBuffer = new ConcurrentHashMap<>();

    public void record(LogEvent logEvent) {
        String key = logEvent.getHttpMethod() + ":" + logEvent.getEndpoint();
        eventBuffer.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(logEvent);
    }

    @Scheduled(fixedRate = 60000) // runs every 60 seconds
    public void flush() {
        if (eventBuffer.isEmpty()) {
            log.info("No events to flush.");
            return;
        }

        log.info("Flushing metrics for {} endpoints", eventBuffer.size());

        for (Map.Entry<String, List<LogEvent>> entry : eventBuffer.entrySet()) {
            String key = entry.getKey();
            List<LogEvent> events = entry.getValue();

            if (events.isEmpty()) continue;

            // compute stats
            long totalRequests = events.size();
            long errorCount = events.stream()
                    .filter(e -> e.getStatusCode() >= 400)
                    .count();
            double errorRate = (double) errorCount / totalRequests * 100;

            List<Long> latencies = events.stream()
                    .map(LogEvent::getResponseTimeMs)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            double avgLatency = latencies.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);

            long p95Latency = latencies.get((int) Math.ceil(0.95 * latencies.size()) - 1);

            String[] parts = key.split(":", 2);
            String httpMethod = parts[0];
            String endpoint = parts[1];

            log.info("Stats for [{} {}] → requests={} avgLatency={}ms p95={}ms errorRate={}%",
                    httpMethod, endpoint, totalRequests, avgLatency, p95Latency, errorRate);

            // write to InfluxDB
            influxDBWriter.write(endpoint, httpMethod, totalRequests, avgLatency, p95Latency, errorRate);

            // check for anomalies
            anomalyDetector.check(endpoint, httpMethod, p95Latency, errorRate);
        }

        // clear the buffer after flush
        eventBuffer.clear();
    }
}