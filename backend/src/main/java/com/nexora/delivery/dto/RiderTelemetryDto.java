package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Real-time rider telemetry snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderTelemetryDto implements Serializable {
    private Long riderId;
    private double latitude;
    private double longitude;
    private Double speedKmh;
    private Double heading;
    private long updatedAtEpochMillis;
    private boolean isFresh;
    private long ageSeconds;

    public static boolean isValidCoordinate(double lat, double lng) {
        if (Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }
}
