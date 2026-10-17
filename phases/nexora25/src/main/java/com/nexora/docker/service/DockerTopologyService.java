package com.nexora.docker.service;

import com.nexora.docker.dto.DockerConceptExplanation;
import com.nexora.docker.dto.DockerContainerSpec;
import com.nexora.docker.dto.DockerTopologyResponse;

import java.util.List;

public interface DockerTopologyService {
    List<DockerContainerSpec> getAllContainers();
    DockerTopologyResponse getTopology();
    List<DockerConceptExplanation> getConceptExplanations();
}
