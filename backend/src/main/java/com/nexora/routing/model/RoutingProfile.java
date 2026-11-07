package com.nexora.routing.model;

/**
 * Travel profiles supported by the routing engine.
 */
public enum RoutingProfile {
    DRIVING("driving"),
    BIKE("bike"),
    WALKING("foot");

    private final String osrmProfile;

    RoutingProfile(String osrmProfile) {
        this.osrmProfile = osrmProfile;
    }

    public String getOsrmProfile() {
        return osrmProfile;
    }
}
