package com.nexora.aws.service;

import com.nexora.aws.dto.AwsArchitectureTopology;
import com.nexora.aws.dto.AwsCostEstimate;
import com.nexora.aws.dto.AwsServiceSpec;

import java.util.List;

public interface AwsArchitectureService {
    List<AwsServiceSpec> getSelectedServices();
    AwsArchitectureTopology getTopology();
    AwsCostEstimate getMonthlyCostEstimate();
}
