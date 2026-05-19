package com.monitor.api_health_monitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String endpoint;
    private String httpMethod;
    private String anomalyType;
    private long observedValue;
    private long thresholdValue;

    @Column(length = 2000)
    private String aiExplanation;

    private String severity;
    private Instant detectedAt;
    private boolean slackNotified;
}