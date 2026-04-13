package com.osrsmcp;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * Detects whether Tailscale is running and returns the device's Tailscale IP.
 * Tailscale assigns addresses in the 100.64.0.0/10 CGNAT range.
 */
@Slf4j
@Singleton
public class TailscaleService
{
    private static final String TAILSCALE_PREFIX = "100.";

    /**
     * Returns the Tailscale IPv4 address if Tailscale is active, or null.
     * First checks network interfaces for a 100.x.x.x address (fast, no process spawn).
     * Falls back to running `tailscale ip` if no interface found.
     */
    public String getTailscaleIp()
    {
        // Fast path: scan network interfaces for a 100.x.x.x address
        try
        {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (!iface.isUp()) continue;
                String name = iface.getName().toLowerCase();
                // Tailscale interface is typically named tailscale0, utun (macOS), or ts0
                boolean isTailscaleIface = name.contains("tailscale") || name.startsWith("utun") || name.startsWith("ts");
                if (!isTailscaleIface) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses()))
                {
                    if (addr instanceof Inet4Address && addr.getHostAddress().startsWith(TAILSCALE_PREFIX))
                        return addr.getHostAddress();
                }
            }

            // Also check all interfaces for a 100.x.x.x address regardless of name
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (!iface.isUp()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses()))
                {
                    if (addr instanceof Inet4Address && addr.getHostAddress().startsWith(TAILSCALE_PREFIX))
                        return addr.getHostAddress();
                }
            }
        }
        catch (Exception e)
        {
            log.debug("OSRS MCP: Error scanning interfaces for Tailscale: {}", e.getMessage());
        }

        // Slow path: try running `tailscale ip`
        return getTailscaleIpFromCli();
    }

    private String getTailscaleIpFromCli()
    {
        String[] commands = {"tailscale", "/Applications/Tailscale.app/Contents/MacOS/Tailscale",
            "C:\\Program Files\\Tailscale\\tailscale.exe"};

        for (String cmd : commands)
        {
            try
            {
                Process p = new ProcessBuilder(cmd, "ip", "--4")
                    .redirectErrorStream(true)
                    .start();
                String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .readLine();
                p.waitFor();
                if (output != null && output.trim().startsWith(TAILSCALE_PREFIX))
                    return output.trim();
            }
            catch (Exception ignored) {}
        }
        return null;
    }

    public boolean isRunning()
    {
        return getTailscaleIp() != null;
    }
}
