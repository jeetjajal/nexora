package com.nexora.aws.service;

import com.nexora.aws.dto.AwsArchitectureTopology;
import com.nexora.aws.dto.AwsCostEstimate;
import com.nexora.aws.dto.AwsServiceSpec;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AwsArchitectureServiceImpl implements AwsArchitectureService {

    private final List<AwsServiceSpec> services = Arrays.asList(
            AwsServiceSpec.builder()
                    .serviceName("Application Load Balancer (ALB)")
                    .category("Traffic & Ingress")
                    .awsProduct("AWS ALB")
                    .tierOrInstanceType("Multi-AZ Ingress with AWS WAF")
                    .selectionRationale("Distributes inbound HTTPS traffic across ECS containers, performs TLS termination with ACM, and defends against Layer 7 attacks.")
                    .securityAndIsolation("Deployed in Public Subnets; listens on 443 with HTTPS redirect. Restricts egress strictly to ECS security group.")
                    .publiclyAccessible(true)
                    .multiAz(true)
                    .build(),

            AwsServiceSpec.builder()
                    .serviceName("Container Compute")
                    .category("Compute")
                    .awsProduct("Amazon ECS on AWS Fargate")
                    .tierOrInstanceType("Task: 1 vCPU, 2GB RAM (Autoscaling 2 to 10 tasks)")
                    .selectionRationale("Serverless container execution eliminates EC2 patching/OS maintenance while providing seamless CPU/memory-based autoscaling.")
                    .securityAndIsolation("Deployed in Private Application Subnets; egresses via NAT Gateway. Zero public IP addresses.")
                    .publiclyAccessible(false)
                    .multiAz(true)
                    .build(),

            AwsServiceSpec.builder()
                    .serviceName("Primary Relational Database")
                    .category("Database")
                    .awsProduct("Amazon RDS for MySQL")
                    .tierOrInstanceType("db.t4g.medium (Multi-AZ with Standby Replica)")
                    .selectionRationale("Automatic synchronous replication to secondary AZ, automated backups, point-in-time recovery, and 99.95% SLA.")
                    .securityAndIsolation("Deployed in Data-Isolated Subnets; accepts traffic strictly from ECS security group on port 3306. Public access disabled.")
                    .publiclyAccessible(false)
                    .multiAz(true)
                    .build(),

            AwsServiceSpec.builder()
                    .serviceName("In-Memory Cache & Rate Limiter")
                    .category("Caching")
                    .awsProduct("Amazon ElastiCache for Redis")
                    .tierOrInstanceType("cache.t4g.small (Cluster Mode, 1 Primary + 1 Read Replica)")
                    .selectionRationale("Sub-millisecond latency for session state, cart caching, and distributed token bucket rate limiting with automatic failover.")
                    .securityAndIsolation("Isolated Data Subnet; encrypted at rest (AES-256) and in transit (TLS). Accessible only from ECS security group on port 6379.")
                    .publiclyAccessible(false)
                    .multiAz(true)
                    .build(),

            AwsServiceSpec.builder()
                    .serviceName("Static Media & Product Images")
                    .category("Storage & CDN")
                    .awsProduct("Amazon S3 + Amazon CloudFront")
                    .tierOrInstanceType("S3 Standard + Global Edge Distribution (OAC)")
                    .selectionRationale("Decouples media storage from compute containers, offering infinite durability (99.999999999%) and low latency global edge caching.")
                    .securityAndIsolation("S3 bucket public access is 100% blocked; CloudFront accesses S3 using Origin Access Control (OAC) with signed URLs.")
                    .publiclyAccessible(true)
                    .multiAz(true)
                    .build(),

            AwsServiceSpec.builder()
                    .serviceName("Observability & Logging")
                    .category("Monitoring")
                    .awsProduct("Amazon CloudWatch & Container Insights")
                    .tierOrInstanceType("Centralized Log Groups + Metric Alarms")
                    .selectionRationale("Aggregates container stdout/stderr, CPU/memory alarms, and triggers SNS notifications for SRE incident response.")
                    .securityAndIsolation("IAM roles grant ECS tasks write-only access to log groups via VPC Endpoints without traversing internet.")
                    .publiclyAccessible(false)
                    .multiAz(true)
                    .build()
    );

    @Override
    public List<AwsServiceSpec> getSelectedServices() {
        return services;
    }

    @Override
    public AwsArchitectureTopology getTopology() {
        return AwsArchitectureTopology.builder()
                .vpcCidr("10.0.0.0/16")
                .region("ap-south-1 (Mumbai)")
                .availabilityZones(Arrays.asList("ap-south-1a", "ap-south-1b"))
                .publicSubnets(Arrays.asList("10.0.1.0/24 (ap-south-1a)", "10.0.2.0/24 (ap-south-1b)"))
                .appPrivateSubnets(Arrays.asList("10.0.11.0/24 (ap-south-1a)", "10.0.12.0/24 (ap-south-1b)"))
                .dataIsolatedSubnets(Arrays.asList("10.0.21.0/24 (ap-south-1a)", "10.0.22.0/24 (ap-south-1b)"))
                .services(services)
                .secretsManagementStrategy("AWS Secrets Manager + KMS envelope encryption; secrets mounted directly into Fargate task environment.")
                .disasterRecoveryRpoRto("RPO < 5 minutes (Automated RDS snapshots & transaction logs), RTO < 15 minutes (Multi-AZ automated failover).")
                .build();
    }

    @Override
    public AwsCostEstimate getMonthlyCostEstimate() {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("Amazon ECS Fargate (2 Tasks Baseline)", 48.50);
        breakdown.put("Amazon RDS MySQL Multi-AZ (db.t4g.medium)", 78.40);
        breakdown.put("Amazon ElastiCache Redis (cache.t4g.small)", 34.20);
        breakdown.put("Application Load Balancer (ALB + LCU)", 22.50);
        breakdown.put("Amazon S3 & CloudFront CDN (50GB + 200GB Egress)", 14.80);
        breakdown.put("NAT Gateway (2 AZs Data Processing)", 64.80);
        breakdown.put("AWS CloudWatch Logs & Metrics", 11.20);

        double totalUsd = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalInr = totalUsd * 83.50; // Conversion rate

        return AwsCostEstimate.builder()
                .environment("Production (ap-south-1)")
                .monthlyTotalUsd(Math.round(totalUsd * 100.0) / 100.0)
                .monthlyTotalInr(Math.round(totalInr * 100.0) / 100.0)
                .breakdownByServiceUsd(breakdown)
                .costOptimizationAdvice("Use AWS Savings Plans for ECS Fargate (saves up to 45%), Reserved Instances for RDS Multi-AZ, and VPC Endpoints for S3/DynamoDB to eliminate NAT Gateway egress costs.")
                .build();
    }
}
