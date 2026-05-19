package com.monitor.api_health_monitor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monitor.api_health_monitor.model.LogEvent;
import com.monitor.api_health_monitor.service.MetricsAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final MetricsAggregator metricsAggregator;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @KafkaListener(topics = "api-logs", groupId = "api-monitor-group")
    public void consume(String message) {
        try {
            LogEvent logEvent = objectMapper.readValue(message, LogEvent.class);
            log.info("Consumed from Kafka: endpoint={} status={} latency={}ms",
                    logEvent.getEndpoint(),
                    logEvent.getStatusCode(),
                    logEvent.getResponseTimeMs());
            metricsAggregator.record(logEvent);
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
        }
    }
}