package com.monitor.api_health_monitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
            "message", "Hello from API Health Monitor",
            "status", "running"
        );
    }

    @GetMapping("/slow")
    public Map<String, String> slow() throws InterruptedException {
        Thread.sleep(2000);
        return Map.of(
            "message", "This was a slow response",
            "status", "done"
        );
    }

    @GetMapping("/error")
    public Map<String, String> error() {
        throw new RuntimeException("Simulated error for testing");
    }
}