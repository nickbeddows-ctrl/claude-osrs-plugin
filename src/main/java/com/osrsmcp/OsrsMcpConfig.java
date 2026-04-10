package com.osrsmcp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("osrsmcp")
public interface OsrsMcpConfig extends Config
{
    @ConfigSection(name = "Connection", description = "MCP server connection settings", position = 0)
    String connectionSection = "connection";

    @ConfigItem(keyName = "port", name = "Port",
        description = "Port the local MCP server listens on. Restart the plugin after changing.",
        section = connectionSection, position = 0)
    default int port() { return 8282; }

    @ConfigItem(keyName = "allowLan", name = "Allow LAN connections",
        description = "Bind to all network interfaces so devices on the same subnet can connect. " +
                      "Set an Auth Token if you enable this.",
        section = connectionSection, position = 1)
    default boolean allowLan() { return false; }

    @ConfigItem(keyName = "relayEnabled", name = "Cloud relay",
        description = "Creates a public HTTPS tunnel via SSH so any device can connect regardless " +
                      "of network. Uses serveo.net with localhost.run as fallback. " +
                      "SSH must be available (built into macOS and Windows 10/11). " +
                      "The relay URL is shown in the plugin panel.",
        section = connectionSection, position = 2)
    default boolean relayEnabled() { return false; }

    @ConfigItem(keyName = "authToken", name = "Auth Token",
        description = "Optional Bearer token. Recommended when LAN or Cloud Relay is enabled.",
        section = connectionSection, position = 3, secret = true)
    default String authToken() { return ""; }

    @ConfigSection(name = "Privacy", description = "Control what data the AI can access", position = 1)
    String privacySection = "privacy";

    @ConfigItem(keyName = "shareStats", name = "Share skill levels & XP",
        description = "Allow the AI to read your skill levels and experience",
        section = privacySection, position = 0)
    default boolean shareStats() { return true; }

    @ConfigItem(keyName = "shareEquipment", name = "Share equipped gear",
        description = "Allow the AI to see what items you have equipped",
        section = privacySection, position = 1)
    default boolean shareEquipment() { return true; }

    @ConfigItem(keyName = "shareInventory", name = "Share inventory",
        description = "Allow the AI to see your inventory contents",
        section = privacySection, position = 2)
    default boolean shareInventory() { return true; }

    @ConfigItem(keyName = "shareLocation", name = "Share location",
        description = "Allow the AI to see your current in-game location",
        section = privacySection, position = 3)
    default boolean shareLocation() { return true; }

    @ConfigItem(keyName = "shareUsername", name = "Share username",
        description = "Include your RSN in responses. Disable for privacy.",
        section = privacySection, position = 4)
    default boolean shareUsername() { return true; }
}
