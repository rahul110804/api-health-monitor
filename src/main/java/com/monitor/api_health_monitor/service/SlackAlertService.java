package com.monitor.api_health_monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class SlackAlertService {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean sendAlert(String endpoint, String httpMethod,
                              String anomalyType, long observedValue,
                              long thresholdValue, String aiExplanation) {
        try {
            String emoji = anomalyType.equals("HIGH_LATENCY") ? "🐢" : "🔴";
            String title = anomalyType.equals("HIGH_LATENCY")
                    ? "High Latency Detected"
                    : "High Error Rate Detected";

            String metric = anomalyType.equals("HIGH_LATENCY")
                    ? String.format("p95 Latency: *%dms* (threshold: %dms)",
                                    observedValue, thresholdValue)
                    : String.format("Error Rate: *%d%%* (threshold: %d%%)",
                                    observedValue, thresholdValue);

            String message = String.format("""
                {
                    "blocks": [
                        {
                            "type": "header",
                            "text": {
                                "type": "plain_text",
                                "text": "%s %s"
                            }
                        },
                        {
                            "type": "section",
                            "fields": [
                                {
                                    "type": "mrkdwn",
                                    "text": "*Endpoint:*\\n%s %s"
                                },
                                {
                                    "type": "mrkdwn",
                                    "text": "*Metric:*\\n%s"
                                }
                            ]
                        },
                        {
                            "type": "section",
                            "text": {
                                "type": "mrkdwn",
                                "text": "*AI Diagnosis:*\\n%s"
                            }
                        },
                        {
                            "type": "divider"
                        }
                    ]
                }
                """,
                emoji, title,
                httpMethod, endpoint,
                metric,
                aiExplanation.replace("\"", "'").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Slack alert sent for endpoint: {}", endpoint);
                return true;
            } else {
                log.error("Slack webhook error: status={} body={}",
                          response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            log.error("Failed to send Slack alert", e);
            return false;
        }
    }
}