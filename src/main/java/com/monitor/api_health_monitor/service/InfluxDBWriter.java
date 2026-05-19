package com.monitor.api_health_monitor.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class InfluxDBWriter {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String influxOrg;

    public void write(String endpoint,
                      String httpMethod,
                      long totalRequests,
                      double avgLatency,
                      long p95Latency,
                      double errorRate) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("api_metrics")
                    .addTag("endpoint", endpoint)
                    .addTag("http_method", httpMethod)
                    .addTag("service", "api-health-monitor")
                    .addField("total_requests", totalRequests)
                    .addField("avg_latency_ms", avgLatency)
                    .addField("p95_latency_ms", p95Latency)
                    .addField("error_rate_pct", errorRate)
                    .time(Instant.now(), WritePrecision.MS);

            writeApi.writePoint(bucket, influxOrg, point);

            log.info("Written to InfluxDB: endpoint={} p95={}ms errorRate={}%",
                    endpoint, p95Latency, errorRate);

        } catch (Exception e) {
            log.error("Failed to write to InfluxDB for endpoint: {}", endpoint, e);
        }
    }
}