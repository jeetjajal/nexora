package com.nexora.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Encapsulates the entire multi-container architecture layout and container specs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerTopologyResponse {
    private String composeVersion;
    private String networkName;
    private List<DockerContainerSpec> containers;
    private List<String> volumes;
    private List<DockerConceptExplanation> concepts;
    private String securitySummary;
}
