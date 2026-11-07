package com.nexora.routing.dto;

import com.nexora.routing.model.RoutePoint;
import com.nexora.routing.model.RoutingProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest implements Serializable {
    private RoutePoint origin;
    private RoutePoint destination;
    @Builder.Default
    private RoutingProfile profile = RoutingProfile.DRIVING;

    public boolean isValid() {
        return origin != null && origin.isValid() && destination != null && destination.isValid();
    }
}
