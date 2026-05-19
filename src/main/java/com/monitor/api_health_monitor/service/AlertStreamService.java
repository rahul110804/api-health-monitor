package com.monitor.api_health_monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monitor.api_health_monitor.model.AnomalyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class AlertStreamService {

    // all connected browser clients
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        emitters.add(emitter);
        log.info("New browser client connected. Total clients: {}", emitters.size());

        // send a heartbeat immediately so browser knows connection is alive
        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("connected"));
        } catch (Exception e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(AnomalyEvent anomaly, String aiExplanation) {
        if (emitters.isEmpty()) return;

        try {
            Map<String, Object> payload = Map.of(
                    "endpoint", anomaly.getEndpoint(),
                    "httpMethod", anomaly.getHttpMethod(),
                    "type", anomaly.getType(),
                    "message", anomaly.getMessage(),
                    "observedValue", anomaly.getObservedValue(),
                    "thresholdValue", anomaly.getThresholdValue(),
                    "aiExplanation", aiExplanation
            );

            String json = objectMapper.writeValueAsString(payload);

            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("anomaly")
                            .data(json));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            emitters.removeAll(deadEmitters);
            log.info("Broadcasted anomaly to {} clients", emitters.size());

        } catch (Exception e) {
            log.error("Failed to broadcast anomaly", e);
        }
    }
}