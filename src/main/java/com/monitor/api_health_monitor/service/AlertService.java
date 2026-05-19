package com.monitor.api_health_monitor.service;

import com.monitor.api_health_monitor.model.Alert;
import com.monitor.api_health_monitor.model.AnomalyEvent;
import com.monitor.api_health_monitor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j

public class AlertService {

        private final GroqApiService groqApiService;
        private final SlackAlertService slackAlertService;
        private final AlertRepository alertRepository;
        private final AlertStreamService alertStreamService;

        // cooldown tracker — endpoint:anomalyType → last alert time
        private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();
        private static final long COOLDOWN_MINUTES = 10;

        public void handleAnomaly(AnomalyEvent anomaly) {
                String cooldownKey = anomaly.getEndpoint() + ":" + anomaly.getType();

                // check cooldown — don't spam alerts
                Instant lastAlert = lastAlertTime.get(cooldownKey);
                if (lastAlert != null) {
                        long minutesSinceLast = java.time.Duration.between(lastAlert, Instant.now()).toMinutes();
                        if (minutesSinceLast < COOLDOWN_MINUTES) {
                                log.info("Alert suppressed for {} — cooldown active ({} min remaining)",
                                                anomaly.getEndpoint(), COOLDOWN_MINUTES - minutesSinceLast);
                                return;
                        }
                }

                log.info("Handling anomaly for endpoint: {} type: {}",
                                anomaly.getEndpoint(), anomaly.getType());

                // Step 1 — get AI explanation from Groq
                String aiExplanation = groqApiService.getExplanation(
                                anomaly.getEndpoint(),
                                anomaly.getHttpMethod(),
                                anomaly.getType(),
                                anomaly.getObservedValue(),
                                anomaly.getThresholdValue());

                log.info("AI Explanation: {}", aiExplanation);

                // broadcast to browser in real time
                alertStreamService.broadcast(anomaly, aiExplanation);

                // Step 2 — send Slack alert
                boolean slackSent = slackAlertService.sendAlert(
                                anomaly.getEndpoint(),
                                anomaly.getHttpMethod(),
                                anomaly.getType(),
                                anomaly.getObservedValue(),
                                anomaly.getThresholdValue(),
                                aiExplanation);

                // Step 3 — save to PostgreSQL
                Alert alert = Alert.builder()
                                .endpoint(anomaly.getEndpoint())
                                .httpMethod(anomaly.getHttpMethod())
                                .anomalyType(anomaly.getType())
                                .observedValue(anomaly.getObservedValue())
                                .thresholdValue(anomaly.getThresholdValue())
                                .aiExplanation(aiExplanation)
                                .severity(anomaly.getObservedValue() > anomaly.getThresholdValue() * 2
                                                ? "CRITICAL"
                                                : "WARNING")
                                .detectedAt(Instant.now())
                                .slackNotified(slackSent)
                                .build();

                alertRepository.save(alert);
                log.info("Alert saved to PostgreSQL: {}", alert.getId());

                // update cooldown tracker
                lastAlertTime.put(cooldownKey, Instant.now());
        }
}