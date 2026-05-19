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
public class GroqApiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String getExplanation(String endpoint, String httpMethod,
                                  String anomalyType, long observedValue,
                                  long thresholdValue) {
        try {
            String prompt = buildPrompt(endpoint, httpMethod, anomalyType,
                                        observedValue, thresholdValue);

            String requestBody = """
                {
                    "model": "llama-3.3-70b-versatile",
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are an expert backend engineer specializing in API performance debugging. Give concise, actionable diagnoses in 3-4 sentences max."
                        },
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "max_tokens": 300
                }
                """.formatted(prompt.replace("\"", "'"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                log.error("Groq API error: status={} body={}", 
                          response.statusCode(), response.body());
                return fallbackExplanation(anomalyType, endpoint);
            }

        } catch (Exception e) {
            log.error("Failed to call Groq API", e);
            return fallbackExplanation(anomalyType, endpoint);
        }
    }

    private String buildPrompt(String endpoint, String httpMethod,
                                String anomalyType, long observedValue,
                                long thresholdValue) {
        if (anomalyType.equals("HIGH_LATENCY")) {
            return String.format(
                "The API endpoint %s %s has a p95 latency of %dms, " +
                "which exceeds the threshold of %dms. " +
                "What are the most likely root causes and what should " +
                "the developer check first to debug this?",
                httpMethod, endpoint, observedValue, thresholdValue);
        } else {
            return String.format(
                "The API endpoint %s %s has an error rate of %d%%, " +
                "which exceeds the threshold of %d%%. " +
                "What are the most likely root causes and what should " +
                "the developer check first to debug this?",
                httpMethod, endpoint, observedValue, thresholdValue);
        }
    }

    private String extractContent(String responseBody) {
        try {
            int contentStart = responseBody.indexOf("\"content\":\"") + 11;
            int contentEnd = responseBody.indexOf("\"", contentStart);
            if (contentStart > 11 && contentEnd > contentStart) {
                return responseBody.substring(contentStart, contentEnd);
            }
            // fallback — find content differently
            String marker = "\"content\": \"";
            int idx = responseBody.indexOf(marker);
            if (idx != -1) {
                int start = idx + marker.length();
                int end = responseBody.indexOf("\",", start);
                if (end != -1) return responseBody.substring(start, end);
            }
            return "AI explanation unavailable — check logs for details.";
        } catch (Exception e) {
            log.error("Failed to parse Groq response", e);
            return "AI explanation unavailable — check logs for details.";
        }
    }

    private String fallbackExplanation(String anomalyType, String endpoint) {
        if (anomalyType.equals("HIGH_LATENCY")) {
            return String.format(
                "High latency detected on %s. " +
                "Check database query performance, external service calls, " +
                "and thread pool saturation as likely causes.", endpoint);
        } else {
            return String.format(
                "High error rate detected on %s. " +
                "Check recent deployments, authentication issues, " +
                "and downstream service health.", endpoint);
        }
    }
}