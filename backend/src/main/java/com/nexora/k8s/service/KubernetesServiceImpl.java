package com.nexora.k8s.service;

import com.nexora.k8s.dto.HpaSimulationRequest;
import com.nexora.k8s.dto.HpaSimulationResult;
import com.nexora.k8s.dto.K8sConceptDetail;
import com.nexora.k8s.dto.K8sResourceSpec;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class KubernetesServiceImpl implements KubernetesService {

    private final List<K8sResourceSpec> resources = Arrays.asList(
            K8sResourceSpec.builder()
                    .kind("Deployment")
                    .name("nexora-order-service")
                    .namespace("nexora-production")
                    .replicas(1)
                    .status("RUNNING (1/1 Ready)")
                    .resourceRequests("CPU: 250m, Memory: 512Mi")
                    .resourceLimits("CPU: 1000m, Memory: 1024Mi")
                    .livenessProbe("HTTP GET /actuator/health/liveness (delay 45s, period 10s)")
                    .readinessProbe("HTTP GET /actuator/health/readiness (delay 25s, period 5s)")
                    .notes(Arrays.asList("Runs as non-root user (UID 10001)", "RollingUpdate strategy: maxSurge 1, maxUnavailable 0"))
                    .build(),

            K8sResourceSpec.builder()
                    .kind("HorizontalPodAutoscaler")
                    .name("nexora-order-service-hpa")
                    .namespace("nexora-production")
                    .replicas(1)
                    .status("ACTIVE (Target: 70% CPU, Range: 1 - 5 Pods)")
                    .resourceRequests("N/A")
                    .resourceLimits("N/A")
                    .livenessProbe("N/A")
                    .readinessProbe("N/A")
                    .notes(Arrays.asList("Scales 1 -> 3 -> 5 pods during traffic surges", "Cooldown stabilization window: 300s"))
                    .build(),

            K8sResourceSpec.builder()
                    .kind("Service")
                    .name("nexora-order-service")
                    .namespace("nexora-production")
                    .replicas(1)
                    .status("ClusterIP (Internal Virtual IP: 10.96.12.45)")
                    .resourceRequests("N/A")
                    .resourceLimits("N/A")
                    .livenessProbe("N/A")
                    .readinessProbe("N/A")
                    .notes(Collections.singletonList("Exposes port 80 -> container port 8080 with round-robin load balancing"))
                    .build(),

            K8sResourceSpec.builder()
                    .kind("Ingress")
                    .name("nexora-ingress")
                    .namespace("nexora-production")
                    .replicas(1)
                    .status("ROUTING (api.nexora.com)")
                    .resourceRequests("N/A")
                    .resourceLimits("N/A")
                    .livenessProbe("N/A")
                    .readinessProbe("N/A")
                    .notes(Arrays.asList("Routes /api/v1/orders to order-service", "TLS termination via secret nexora-tls-cert"))
                    .build()
    );

    private final List<K8sConceptDetail> concepts = Arrays.asList(
            K8sConceptDetail.builder()
                    .concept("Pods & Deployments")
                    .roleInNexora("A Pod is the smallest deployable compute unit enclosing the container. Deployments manage declarative state, rolling upgrades, and replica counts.")
                    .failureHandling("If a Pod crashes or container process dies, Kubelet automatically restarts it; if node fails, Deployment reschedules pod on healthy node.")
                    .interviewTip("Always define resource requests and limits to prevent noisy neighbor pods from starving node resources (OOMKilled).")
                    .build(),

            K8sConceptDetail.builder()
                    .concept("Liveness vs Readiness Probes")
                    .roleInNexora("Liveness monitors if the JVM is deadlocked/crashed (triggers restart). Readiness checks if DB/Kafka connections are established before sending traffic.")
                    .failureHandling("Failing readiness probe temporarily removes pod from Service endpoints so users never receive 502/503 errors while starting up.")
                    .interviewTip("Never check downstream databases in a liveness probe; if the database goes down, all pods would restart simultaneously (cascading failure).")
                    .build(),

            K8sConceptDetail.builder()
                    .concept("Services & Ingress")
                    .roleInNexora("Service provides a stable virtual IP and DNS name across ephemeral pod IPs. Ingress exposes HTTP/HTTPS routes into the cluster from outside.")
                    .failureHandling("Kube-proxy updates iptables/IPVS rules automatically as pods are added or removed.")
                    .interviewTip("Explain ClusterIP (internal), NodePort (debug), LoadBalancer (cloud provider), and Ingress (Layer 7 reverse proxy).")
                    .build(),

            K8sConceptDetail.builder()
                    .concept("Horizontal Pod Autoscaler (HPA)")
                    .roleInNexora("Dynamically scales pod count based on observed CPU/memory consumption (e.g. flash sales, dinner peak hours).")
                    .failureHandling("Scale-up is aggressive (instant), while scale-down has a 5-minute cooldown stabilization window to prevent flapping.")
                    .interviewTip("HPA Formula: desiredReplicas = ceil[currentReplicas * (currentMetricValue / targetMetricValue)].")
                    .build()
    );

    @Override
    public List<K8sResourceSpec> getDeployedResources() {
        return resources;
    }

    @Override
    public HpaSimulationResult simulateHpaScaling(HpaSimulationRequest request) {
        int rps = Math.max(1, request.getIncomingRps());
        int currentPods = Math.max(1, request.getCurrentPods());

        // Baseline: 1 pod comfortably handles 500 RPS at ~35% CPU
        double simulatedCpuPerPod = (double) rps / (currentPods * 500.0) * 70.0;
        double targetUtilization = 70.0;

        // Apply standard Kubernetes HPA autoscaling formula:
        // desiredReplicas = ceil[currentReplicas * (currentUtilization / targetUtilization)]
        int desiredReplicas = (int) Math.ceil(currentPods * (simulatedCpuPerPod / targetUtilization));

        // Clamp between minReplicas (1) and maxReplicas (5)
        desiredReplicas = Math.max(1, Math.min(5, desiredReplicas));

        String decision;
        if (desiredReplicas > currentPods) {
            decision = String.format("SCALE UP: Triggered replica expansion from %d to %d Pods to handle %d RPS surge", currentPods, desiredReplicas, rps);
        } else if (desiredReplicas < currentPods) {
            decision = String.format("SCALE DOWN: Traffic reduced to %d RPS. Scale down to %d Pods after stabilization cooldown", rps, desiredReplicas);
        } else {
            decision = String.format("STEADY STATE: %d Pods sufficient to sustain %d RPS within 70%% CPU threshold", currentPods, rps);
        }

        return HpaSimulationResult.builder()
                .requestedRps(rps)
                .baselinePods(currentPods)
                .scaledPods(desiredReplicas)
                .calculatedCpuPercent(Math.round(simulatedCpuPerPod * 10.0) / 10.0)
                .scalingDecision(decision)
                .hpaFormulaApplied("desiredReplicas = ceil[currentReplicas * (currentCpu / targetCpu(70%))]")
                .rationale(String.format("Order Service automatically scales between 1 and 5 pods. Under %d RPS, required pods = %d.", rps, desiredReplicas))
                .build();
    }

    @Override
    public List<K8sConceptDetail> getCoreConcepts() {
        return concepts;
    }
}
