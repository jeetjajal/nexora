package com.nexora.microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Route mapping configuration for the centralized Spring Cloud API Gateway.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRouteConfig {

    private String routeId;
    private String pathPattern;
    private String targetServiceUri;
    private List<String> appliedFilters;
    private int rateLimitRps;
}
