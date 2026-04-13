package com.osrsmcp;

public enum ConnectionMode
{
    LOCAL("Local"),
    LAN("LAN"),
    TAILSCALE("Tailscale"),
    CLOUD_RELAY("Cloud relay");

    private final String label;

    ConnectionMode(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
