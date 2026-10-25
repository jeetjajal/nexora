package com.nexora.k8s.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sResourceSpec {
    private String kind;
    private String name;
    private String namespace;
    private int replicas;
    private String status;
    private String resourceRequests;
    private String resourceLimits;
    private String livenessProbe;
    private String readinessProbe;
    private List<String> notes;
}
