package com.monitor.api_health_monitor.repository;

import com.monitor.api_health_monitor.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findByEndpointOrderByDetectedAtDesc(String endpoint);
    List<Alert> findTop10ByOrderByDetectedAtDesc();
}