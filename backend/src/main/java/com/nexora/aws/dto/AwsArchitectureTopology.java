package com.nexora.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsArchitectureTopology {
    private String vpcCidr;
    private String region;
    private List<String> availabilityZones;
    private List<String> publicSubnets;
    private List<String> appPrivateSubnets;
    private List<String> dataIsolatedSubnets;
    private List<AwsServiceSpec> services;
    private String secretsManagementStrategy;
    private String disasterRecoveryRpoRto;
}
