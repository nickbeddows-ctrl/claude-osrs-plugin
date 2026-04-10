package com.osrsmcp;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

@Slf4j
@PluginDescriptor(
    name = "OSRS MCP",
    description = "Exposes RuneLite data via a local MCP server for AI-assisted in-game advice.",
    tags = {"claude", "ai", "stats", "helper", "assistant", "mcp"}
)
public class OsrsMcpPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OsrsMcpConfig config;
    @Inject private McpServer mcpServer;
    @Inject private OsrsMcpPanel panel;
    @Inject private RelayService relayService;

    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception
    {
        try
        {
            mcpServer.start(config.port());
            panel.setServerRunning(true, config.port(), config.allowLan() ? getLanIp() : null);
        }
        catch (IOException e)
        {
            log.error("OSRS MCP: Failed to start MCP server on port {}", config.port(), e);
            panel.setError("Port " + config.port() + " is in use. Change it in settings.");
        }

        if (config.relayEnabled())
        {
            panel.setRelayStatus(OsrsMcpPanel.RelayStatus.CONNECTING, null);
            relayService.start(
                config.port(),
                url -> panel.setRelayStatus(OsrsMcpPanel.RelayStatus.ACTIVE, url),
                err -> panel.setRelayStatus(OsrsMcpPanel.RelayStatus.ERROR, err)
            );
        }

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        navButton = NavigationButton.builder()
            .tooltip("OSRS MCP")
            .icon(icon)
            .priority(10)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        relayService.stop();
        mcpServer.stop();
        clientToolbar.removeNavigation(navButton);
        panel.setServerRunning(false, 0, null);
        panel.setRelayStatus(OsrsMcpPanel.RelayStatus.OFF, null);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        panel.updateGameState(event.getGameState());
    }

    @Provides
    OsrsMcpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OsrsMcpConfig.class);
    }

    /**
     * Returns the first non-loopback IPv4 LAN address, or null if none found.
     */
    private String getLanIp()
    {
        try
        {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses()))
                {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                        return addr.getHostAddress();
                }
            }
        }
        catch (Exception e)
        {
            log.warn("OSRS MCP: Could not determine LAN IP", e);
        }
        return null;
    }
}
