package com.monitor.api_health_monitor.filter;

import com.monitor.api_health_monitor.kafka.KafkaProducerService;
import com.monitor.api_health_monitor.model.LogEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RequestInterceptor extends OncePerRequestFilter {

    private final KafkaProducerService kafkaProducerService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            LogEvent logEvent = LogEvent.builder()
                    .serviceName("api-health-monitor")
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .statusCode(response.getStatus())
                    .responseTimeMs(responseTimeMs)
                    .timestamp(Instant.now())
                    .build();

            kafkaProducerService.publish(logEvent);
        }
    }
}