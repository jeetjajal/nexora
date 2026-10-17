package com.nexora.phase23;

import com.nexora.docker.dto.DockerConceptExplanation;
import com.nexora.docker.dto.DockerContainerSpec;
import com.nexora.docker.dto.DockerTopologyResponse;
import com.nexora.docker.service.DockerTopologyService;
import com.nexora.docker.service.DockerTopologyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerTopologyServiceTest {

    private DockerTopologyService dockerTopologyService;

    @BeforeEach
    void setUp() {
        dockerTopologyService = new DockerTopologyServiceImpl();
    }

    @Test
    @DisplayName("Should return specifications for all core containers")
    void testGetAllContainers() {
        List<DockerContainerSpec> containers = dockerTopologyService.getAllContainers();
        assertThat(containers).hasSize(6);

        assertThat(containers).extracting(DockerContainerSpec::getContainerName)
                .containsExactlyInAnyOrder(
                        "nexora-backend",
                        "nexora-mysql",
                        "nexora-redis",
                        "nexora-kafka",
                        "nexora-zookeeper",
                        "nexora-elasticsearch"
                );
    }

    @Test
    @DisplayName("Should ensure persistent storage volumes for stateful containers")
    void testPersistentStorageConfig() {
        List<DockerContainerSpec> containers = dockerTopologyService.getAllContainers();

        DockerContainerSpec mysql = containers.stream()
                .filter(c -> c.getServiceName().equals("mysql"))
                .findFirst()
                .orElseThrow();
        assertThat(mysql.getVolumeMount()).contains("mysql_data");

        DockerContainerSpec redis = containers.stream()
                .filter(c -> c.getServiceName().equals("redis"))
                .findFirst()
                .orElseThrow();
        assertThat(redis.getVolumeMount()).contains("redis_data");

        DockerContainerSpec es = containers.stream()
                .filter(c -> c.getServiceName().equals("elasticsearch"))
                .findFirst()
                .orElseThrow();
        assertThat(es.getVolumeMount()).contains("es_data");
    }

    @Test
    @DisplayName("Should verify full topology response structure")
    void testGetTopology() {
        DockerTopologyResponse topology = dockerTopologyService.getTopology();
        assertThat(topology.getComposeVersion()).isEqualTo("3.8");
        assertThat(topology.getNetworkName()).contains("nexora-net");
        assertThat(topology.getVolumes()).contains("mysql_data", "redis_data", "es_data");
        assertThat(topology.getContainers()).hasSize(6);
        assertThat(topology.getConcepts()).isNotEmpty();
    }

    @Test
    @DisplayName("Should verify Docker concepts contain essential interview guidance")
    void testGetConcepts() {
        List<DockerConceptExplanation> concepts = dockerTopologyService.getConceptExplanations();
        assertThat(concepts).hasSize(5);
        assertThat(concepts).extracting(DockerConceptExplanation::getConcept)
                .contains(
                        "Image vs Container",
                        "Volumes & Data Persistence",
                        "Docker Bridge Networks & DNS",
                        "Port Mapping vs Exposed Ports",
                        "Environment Variables & Secrets"
                );
    }
}
