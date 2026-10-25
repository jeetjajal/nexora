package com.nexora.docker.service;

import com.nexora.docker.dto.DockerConceptExplanation;
import com.nexora.docker.dto.DockerContainerSpec;
import com.nexora.docker.dto.DockerTopologyResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class DockerTopologyServiceImpl implements DockerTopologyService {

    private final List<DockerContainerSpec> containers = Arrays.asList(
            DockerContainerSpec.builder()
                    .containerName("nexora-backend")
                    .serviceName("backend")
                    .image("nexora-backend:latest (eclipse-temurin:17-jre-alpine)")
                    .portMapping("8080:8080")
                    .volumeMount("none (stateless application)")
                    .healthcheck("curl -f http://localhost:8080/actuator/health")
                    .memoryLimit("768MB (MaxRAMPercentage=75%)")
                    .status("HEALTHY")
                    .role("Core Spring Boot Application, REST APIs & WebSocket Server")
                    .envKeys(Arrays.asList("SPRING_DATASOURCE_URL", "SPRING_DATA_REDIS_HOST", "SPRING_KAFKA_BOOTSTRAP_SERVERS", "SPRING_ELASTICSEARCH_URIS", "JWT_SECRET"))
                    .build(),

            DockerContainerSpec.builder()
                    .containerName("nexora-mysql")
                    .serviceName("mysql")
                    .image("mysql:8.0.35")
                    .portMapping("3306:3306")
                    .volumeMount("mysql_data -> /var/lib/mysql")
                    .healthcheck("mysqladmin ping -h localhost")
                    .memoryLimit("1024MB")
                    .status("HEALTHY")
                    .role("Primary ACID Relational Database & Single Source of Truth")
                    .envKeys(Arrays.asList("MYSQL_ROOT_PASSWORD", "MYSQL_DATABASE", "MYSQL_USER", "MYSQL_PASSWORD"))
                    .build(),

            DockerContainerSpec.builder()
                    .containerName("nexora-redis")
                    .serviceName("redis")
                    .image("redis:7.2-alpine")
                    .portMapping("6379:6379")
                    .volumeMount("redis_data -> /data (AOF enabled)")
                    .healthcheck("redis-cli ping")
                    .memoryLimit("512MB")
                    .status("HEALTHY")
                    .role("In-Memory Cache, Rate Limiter Token Buckets & Shopping Carts")
                    .envKeys(Collections.singletonList("REDIS_PORT"))
                    .build(),

            DockerContainerSpec.builder()
                    .containerName("nexora-kafka")
                    .serviceName("kafka")
                    .image("confluentinc/cp-kafka:7.5.0")
                    .portMapping("9092:9092")
                    .volumeMount("ephemeral log segment buffer")
                    .healthcheck("nc -z localhost 9092")
                    .memoryLimit("1024MB")
                    .status("HEALTHY")
                    .role("Distributed Event Streaming Platform for Orders & Delivery Events")
                    .envKeys(Arrays.asList("KAFKA_ZOOKEEPER_CONNECT", "KAFKA_ADVERTISED_LISTENERS", "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR"))
                    .build(),

            DockerContainerSpec.builder()
                    .containerName("nexora-zookeeper")
                    .serviceName("zookeeper")
                    .image("confluentinc/cp-zookeeper:7.5.0")
                    .portMapping("2181:2181")
                    .volumeMount("ephemeral coordination log")
                    .healthcheck("echo ruok | nc localhost 2181")
                    .memoryLimit("256MB")
                    .status("HEALTHY")
                    .role("Distributed Consensus Coordinator for Kafka Broker Metadata")
                    .envKeys(Arrays.asList("ZOOKEEPER_CLIENT_PORT", "ZOOKEEPER_TICK_TIME"))
                    .build(),

            DockerContainerSpec.builder()
                    .containerName("nexora-elasticsearch")
                    .serviceName("elasticsearch")
                    .image("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
                    .portMapping("9200:9200")
                    .volumeMount("es_data -> /usr/share/elasticsearch/data")
                    .healthcheck("curl -f http://localhost:9200/_cluster/health")
                    .memoryLimit("1024MB (-Xms512m -Xmx512m)")
                    .status("HEALTHY")
                    .role("Sub-Second Distributed Full-Text & Fuzzy Catalog Search Engine")
                    .envKeys(Arrays.asList("discovery.type", "xpack.security.enabled", "ES_JAVA_OPTS"))
                    .build()
    );

    private final List<DockerConceptExplanation> concepts = Arrays.asList(
            DockerConceptExplanation.builder()
                    .concept("Image vs Container")
                    .definition("An Image is a read-only, immutable snapshot/blueprint built of stacked filesystem layers. A Container is an isolated, active, runnable instance of an Image with its own thin writable top layer.")
                    .whyItMatters("Eliminates 'it works on my machine' syndrome by ensuring identical environment execution from dev to production.")
                    .bestPractice("Use multi-stage builds and minimal base images (Alpine or Temurin JRE) to keep image footprints small and eliminate build tools from runtime.")
                    .build(),

            DockerConceptExplanation.builder()
                    .concept("Volumes & Data Persistence")
                    .definition("Volumes are Docker-managed storage directories outside the container's copy-on-write filesystem, residing on the host disk.")
                    .whyItMatters("Containers are fundamentally ephemeral; when destroyed or upgraded, all internal state is lost unless bound to a persistent volume.")
                    .bestPractice("Use named volumes (e.g. mysql_data, redis_data) for databases and stateful caches; keep the application container completely stateless.")
                    .build(),

            DockerConceptExplanation.builder()
                    .concept("Docker Bridge Networks & DNS")
                    .definition("A user-defined bridge network isolates container traffic and provides built-in DNS name resolution between services.")
                    .whyItMatters("Backend connects to 'jdbc:mysql://mysql:3306/nexora_db' without needing hardcoded host IPs; Docker automatically routes service names.")
                    .bestPractice("Never rely on default bridge network; create an isolated custom bridge network ('nexora-net') so only intended services can communicate.")
                    .build(),

            DockerConceptExplanation.builder()
                    .concept("Port Mapping vs Exposed Ports")
                    .definition("EXPOSE is container metadata declaring intended listening ports. Port Mapping ('-p 8080:8080') binds host port to container port, making it reachable externally.")
                    .whyItMatters("Inside the Docker network, containers communicate directly using internal ports; external port mapping is only needed for outside clients or debugging.")
                    .bestPractice("In production, only expose the API Gateway or Load Balancer to the public internet; keep databases and Kafka private within the Docker network.")
                    .build(),

            DockerConceptExplanation.builder()
                    .concept("Environment Variables & Secrets")
                    .definition("Dynamic configuration values injected into the container runtime from .env or orchestrators, rather than hardcoded in the codebase.")
                    .whyItMatters("Baking credentials or API keys directly into Docker images leaves them visible in 'docker history' and exposes them across repositories.")
                    .bestPractice("Follow 12-Factor App principles: supply configs via environment variables, commit only '.env.example', and never bake secrets into images.")
                    .build()
    );

    @Override
    public List<DockerContainerSpec> getAllContainers() {
        return containers;
    }

    @Override
    public DockerTopologyResponse getTopology() {
        return DockerTopologyResponse.builder()
                .composeVersion("3.8")
                .networkName("nexora-net (bridge)")
                .containers(containers)
                .volumes(Arrays.asList("mysql_data", "redis_data", "es_data"))
                .concepts(concepts)
                .securitySummary("Zero hardcoded secrets, non-root user (UID 10001), multi-stage build, container healthchecks enabled.")
                .build();
    }

    @Override
    public List<DockerConceptExplanation> getConceptExplanations() {
        return concepts;
    }
}
