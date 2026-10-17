package com.nexora.phase25;

import com.nexora.k8s.dto.HpaSimulationRequest;
import com.nexora.k8s.dto.HpaSimulationResult;
import com.nexora.k8s.dto.K8sResourceSpec;
import com.nexora.k8s.service.KubernetesService;
import com.nexora.k8s.service.KubernetesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KubernetesServiceTest {

    private KubernetesService kubernetesService;

    @BeforeEach
    void setUp() {
        kubernetesService = new KubernetesServiceImpl();
    }

    @Test
    @DisplayName("Should retrieve deployed Kubernetes workloads and specifications")
    void testGetDeployedResources() {
        List<K8sResourceSpec> resources = kubernetesService.getDeployedResources();
        assertThat(resources).hasSize(4);

        assertThat(resources).extracting(K8sResourceSpec::getKind)
                .containsExactlyInAnyOrder("Deployment", "HorizontalPodAutoscaler", "Service", "Ingress");

        K8sResourceSpec deployment = resources.stream()
                .filter(r -> r.getKind().equals("Deployment"))
                .findFirst()
                .orElseThrow();
        assertThat(deployment.getLivenessProbe()).contains("/actuator/health/liveness");
        assertThat(deployment.getReadinessProbe()).contains("/actuator/health/readiness");
        assertThat(deployment.getResourceRequests()).contains("250m");
        assertThat(deployment.getResourceLimits()).contains("1000m");
    }

    @Test
    @DisplayName("Should scale Order Service pods from 1 to 3 to 5 based on incoming traffic surges")
    void testHpaScalingScenarios() {
        // Scenario 1: Normal traffic (400 RPS) -> 1 baseline pod
        HpaSimulationResult resNormal = kubernetesService.simulateHpaScaling(
                HpaSimulationRequest.builder().incomingRps(400).currentPods(1).build()
        );
        assertThat(resNormal.getScaledPods()).isEqualTo(1);
        assertThat(resNormal.getScalingDecision()).contains("STEADY STATE");

        // Scenario 2: Peak rush (1200 RPS) -> Scales to 3 pods
        HpaSimulationResult resPeak = kubernetesService.simulateHpaScaling(
                HpaSimulationRequest.builder().incomingRps(1200).currentPods(1).build()
        );
        assertThat(resPeak.getScaledPods()).isEqualTo(3);
        assertThat(resPeak.getScalingDecision()).contains("SCALE UP");

        // Scenario 3: Extreme flash sale (2500+ RPS) -> Scales to max limit (5 pods)
        HpaSimulationResult resFlashSale = kubernetesService.simulateHpaScaling(
                HpaSimulationRequest.builder().incomingRps(2500).currentPods(1).build()
        );
        assertThat(resFlashSale.getScaledPods()).isEqualTo(5);
        assertThat(resFlashSale.getScalingDecision()).contains("SCALE UP");
    }
}
