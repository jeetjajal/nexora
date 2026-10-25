package com.nexora.microservice.dto;

import com.nexora.microservice.model.MicroserviceId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Architectural specification of a single Nexora Microservice.
 * Enforces the roadmap requirement:
 * "For every service explain responsibility, reason for separation, API, database, communication and failure handling."
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MicroserviceDefinition {

    private MicroserviceId id;
    private String name;
    private int defaultPort;
    private String responsibility;
    private String reasonForSeparation;
    private List<String> primaryEndpoints;
    private String databaseSchema;
    private String communicationType;
    private String failureHandlingStrategy;
    private String status;
}
