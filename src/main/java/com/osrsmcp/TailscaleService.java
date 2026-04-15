package com.osrsmcp;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * Detects whether Tailscale is running by scanning network interfaces for a
 * 100.64.0.0/10 address. No process execution -- pure Java networking.
 */
@Slf4j
@Singleton
public class TailscaleService
{
    private static final String TAILSCALE_PREFIX = "100.";

    public String getTailscaleIp()
    {
        try
        {
            // First pass: look for a Tailscale-named interface
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (!iface.isUp()) continue;
                String name = iface.getName().toLowerCase();
                boolean isTailscaleIface = name.contains("tailscale")
                    || name.startsWith("utun")
                    || name.startsWith("ts");
                if (!isTailscaleIface) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses()))
                    if (addr instanceof Inet4Address && addr.getHostAddress().startsWith(TAILSCALE_PREFIX))
                        return addr.getHostAddress();
            }

            // Second pass: any interface with a 100.x.x.x address
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (!iface.isUp()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses()))
                    if (addr instanceof Inet4Address && addr.getHostAddress().startsWith(TAILSCALE_PREFIX))
                        return addr.getHostAddress();
            }
        }
        catch (Exception e)
        {
            log.debug("OSRS MCP: Error scanning interfaces for Tailscale: {}", e.getMessage());
        }
        return null;
    }

    public boolean isRunning()
    {
        return getTailscaleIp() != null;
    }
}
