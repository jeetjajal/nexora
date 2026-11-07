package com.nexora.routing.controller;

import com.nexora.common.ApiResponse;
import com.nexora.routing.dto.RouteResponse;
import com.nexora.routing.model.RoutingProfile;
import com.nexora.routing.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery/routes")
@RequiredArgsConstructor
public class RoutePreviewController {

    private final RoutingService routingService;

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER','STORE_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> previewRoute(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng,
            @RequestParam(required = false, defaultValue = "DRIVING") RoutingProfile profile) {

        RouteResponse response = routingService.calculateRoute(originLat, originLng, destLat, destLng, profile);
        return ResponseEntity.ok(ApiResponse.success("Route preview calculated successfully", response));
    }
}
