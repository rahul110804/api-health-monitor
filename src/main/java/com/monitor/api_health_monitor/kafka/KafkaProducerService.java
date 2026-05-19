package com.monitor.api_health_monitor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monitor.api_health_monitor.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String TOPIC = "api-logs";

    public void publish(LogEvent logEvent) {
        try {
            String message = objectMapper.writeValueAsString(logEvent);
            kafkaTemplate.send(TOPIC, message);
            log.info("Published to Kafka: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish log event to Kafka", e);
        }
    }
}