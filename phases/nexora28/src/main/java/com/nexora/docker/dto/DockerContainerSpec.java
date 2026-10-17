package com.nexora.docker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Specification and metadata for a container in the Nexora Docker Compose stack.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerContainerSpec {
    private String containerName;
    private String serviceName;
    private String image;
    private String portMapping;
    private String volumeMount;
    private String healthcheck;
    private String memoryLimit;
    private String status;
    private String role;
    private List<String> envKeys;
}
