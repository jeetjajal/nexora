package com.nexora.systemdesign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureScenarioMatrix {
    private String failureEvent;
    private String impact;
    private String automatedMitigation;
    private String recoveryProtocol;
    private String dataConsistencyGuarantee;
}
