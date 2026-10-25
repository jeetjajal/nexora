package com.nexora.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStepDetail {

    private int stepNumber;
    private String microservice;
    private String action;
    private String status;
    private boolean compensating;
    private String details;
}
