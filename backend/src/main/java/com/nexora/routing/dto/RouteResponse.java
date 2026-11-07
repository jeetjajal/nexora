package com.nexora.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse implements Serializable {
    private double distanceMeters;
    private int durationSeconds;
    private String geometry;
    private String routingProvider;
    private boolean isFallbackEstimate;
    private String routeStatus;

    public double getDistanceKm() {
        return distanceMeters / 1000.0;
    }

    public int getDurationMinutes() {
        return (int) Math.ceil(durationSeconds / 60.0);
    }
}
