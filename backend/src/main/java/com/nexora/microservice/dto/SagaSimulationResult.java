package com.nexora.microservice.dto;

import com.nexora.microservice.model.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaSimulationResult {

    private String sagaId;
    private SagaStatus status;
    private boolean overallSuccess;
    private List<SagaStepDetail> executionSteps;
    private String summary;
    private String compensationSummary;
}
