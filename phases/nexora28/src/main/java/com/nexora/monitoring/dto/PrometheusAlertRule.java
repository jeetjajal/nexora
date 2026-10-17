package com.nexora.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusAlertRule {
    private String alertName;
    private String severity;
    private String expression;
    private String duration;
    private String summary;
    private String description;
    private String status;
}
