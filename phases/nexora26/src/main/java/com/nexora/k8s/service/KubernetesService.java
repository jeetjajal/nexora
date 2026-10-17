package com.nexora.k8s.service;

import com.nexora.k8s.dto.HpaSimulationRequest;
import com.nexora.k8s.dto.HpaSimulationResult;
import com.nexora.k8s.dto.K8sConceptDetail;
import com.nexora.k8s.dto.K8sResourceSpec;

import java.util.List;

public interface KubernetesService {
    List<K8sResourceSpec> getDeployedResources();
    HpaSimulationResult simulateHpaScaling(HpaSimulationRequest request);
    List<K8sConceptDetail> getCoreConcepts();
}
