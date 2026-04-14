package com.osrsmcp;

public enum ConnectionMode
{
    LOCAL("Local"),
    LAN("LAN"),
    TAILSCALE("Tailscale");

    private final String label;

    ConnectionMode(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
