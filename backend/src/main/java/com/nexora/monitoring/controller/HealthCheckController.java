package com.nexora.monitoring.controller;

import com.nexora.common.ApiResponse;
import com.nexora.common.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PRODUCTION READINESS & HEALTH ENDPOINT.
 *
 * Performs authoritative probe of application uptime, database connectivity,
 * and memory metrics without exposing credentials.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataSource dataSource;
    private final Environment environment;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean dbHealthy = false;

        try (Connection conn = dataSource.getConnection()) {
            dbHealthy = conn.isValid(2);
        } catch (Exception ex) {
            dbHealthy = false;
        }

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Runtime runtime = Runtime.getRuntime();
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);

        details.put("status", dbHealthy ? "UP" : "DEGRADED");
        details.put("timestamp", Instant.now().toString());
        details.put("database", dbHealthy ? "CONNECTED" : "UNAVAILABLE");
        details.put("uptimeSeconds", uptimeMs / 1000);
        details.put("memory", Map.of(
                "freeMb", freeMem,
                "totalMb", totalMem,
                "usedMb", totalMem - freeMem
        ));
        details.put("profiles", environment.getActiveProfiles());
        details.put("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));

        HttpStatus status = dbHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Health check completed", details));
    }
}
