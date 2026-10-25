package com.nexora.systemdesign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemArchitectureDiagram {
    private String architectureType;
    private String description;
    private List<String> clientLayers;
    private List<String> gatewayLayers;
    private List<String> applicationServices;
    private List<String> asynchronousMessaging;
    private List<String> datastores;
    private Map<String, String> dataFlowSummary;
}
