package com.nexora.phase24;

import com.nexora.aws.dto.AwsArchitectureTopology;
import com.nexora.aws.dto.AwsCostEstimate;
import com.nexora.aws.dto.AwsServiceSpec;
import com.nexora.aws.service.AwsArchitectureService;
import com.nexora.aws.service.AwsArchitectureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AwsArchitectureServiceTest {

    private AwsArchitectureService awsArchitectureService;

    @BeforeEach
    void setUp() {
        awsArchitectureService = new AwsArchitectureServiceImpl();
    }

    @Test
    @DisplayName("Should enforce zero public accessibility for database and caching services")
    void testZeroPublicDatabaseExposure() {
        List<AwsServiceSpec> services = awsArchitectureService.getSelectedServices();

        AwsServiceSpec rds = services.stream()
                .filter(s -> s.getAwsProduct().contains("RDS"))
                .findFirst()
                .orElseThrow();
        assertThat(rds.isPubliclyAccessible()).isFalse();
        assertThat(rds.isMultiAz()).isTrue();

        AwsServiceSpec redis = services.stream()
                .filter(s -> s.getAwsProduct().contains("ElastiCache"))
                .findFirst()
                .orElseThrow();
        assertThat(redis.isPubliclyAccessible()).isFalse();
    }

    @Test
    @DisplayName("Should verify ALB is public and terminates traffic with Multi-AZ redundancy")
    void testAlbConfiguration() {
        List<AwsServiceSpec> services = awsArchitectureService.getSelectedServices();

        AwsServiceSpec alb = services.stream()
                .filter(s -> s.getAwsProduct().contains("ALB"))
                .findFirst()
                .orElseThrow();
        assertThat(alb.isPubliclyAccessible()).isTrue();
        assertThat(alb.isMultiAz()).isTrue();
    }

    @Test
    @DisplayName("Should verify VPC CIDR and subnet tiers in architecture topology")
    void testTopologySubnets() {
        AwsArchitectureTopology topology = awsArchitectureService.getTopology();

        assertThat(topology.getVpcCidr()).isEqualTo("10.0.0.0/16");
        assertThat(topology.getRegion()).contains("ap-south-1");
        assertThat(topology.getAvailabilityZones()).contains("ap-south-1a", "ap-south-1b");
        assertThat(topology.getPublicSubnets()).hasSize(2);
        assertThat(topology.getAppPrivateSubnets()).hasSize(2);
        assertThat(topology.getDataIsolatedSubnets()).hasSize(2);
        assertThat(topology.getSecretsManagementStrategy()).contains("Secrets Manager");
    }

    @Test
    @DisplayName("Should verify monthly cost estimate and FinOps breakdown")
    void testCostEstimate() {
        AwsCostEstimate costEstimate = awsArchitectureService.getMonthlyCostEstimate();

        assertThat(costEstimate.getMonthlyTotalUsd()).isGreaterThan(200.0);
        assertThat(costEstimate.getMonthlyTotalInr()).isGreaterThan(16000.0);
        assertThat(costEstimate.getBreakdownByServiceUsd()).containsKey("Amazon RDS MySQL Multi-AZ (db.t4g.medium)");
        assertThat(costEstimate.getCostOptimizationAdvice()).isNotEmpty();
    }
}
