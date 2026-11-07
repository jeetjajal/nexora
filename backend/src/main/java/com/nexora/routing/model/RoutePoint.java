package com.nexora.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Geographic coordinate point for route calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoint implements Serializable {
    private double latitude;
    private double longitude;

    public boolean isValid() {
        if (Math.abs(latitude) < 0.0001 && Math.abs(longitude) < 0.0001) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    public static RoutePoint of(double latitude, double longitude) {
        return new RoutePoint(latitude, longitude);
    }
}
