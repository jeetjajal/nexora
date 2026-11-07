package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRiderDto implements Serializable {
    private Long riderId;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private boolean isFresh;
    private long ageSeconds;
}
