package com.nexora.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Specification and rationale for an AWS managed service selected for Nexora's cloud architecture.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsServiceSpec {
    private String serviceName;
    private String category;
    private String awsProduct;
    private String tierOrInstanceType;
    private String selectionRationale;
    private String securityAndIsolation;
    private boolean publiclyAccessible;
    private boolean multiAz;
}
