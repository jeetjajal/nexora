package com.nexora.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrafanaDashboardSpec {
    private String title;
    private String uid;
    private int refreshIntervalSeconds;
    private List<String> panels;
    private String prometheusDatasource;
}
