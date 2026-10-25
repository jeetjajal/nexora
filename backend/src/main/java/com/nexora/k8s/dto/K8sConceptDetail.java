package com.nexora.k8s.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sConceptDetail {
    private String concept;
    private String roleInNexora;
    private String failureHandling;
    private String interviewTip;
}
