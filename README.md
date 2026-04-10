# OSRS MCP

Exposes your RuneLite client data via a local [MCP (Model Context Protocol)](https://modelcontextprotocol.io) server, letting any MCP-compatible AI assistant see your stats, gear, inventory and location to give context-aware in-game advice.

> **Origin:** This plugin was originally built with [Claude AI](https://claude.ai) in mind, but MCP is an open standard supported by a growing number of AI tools. Any MCP-compatible assistant can connect to it.

## What it does

Ask your AI assistant things like:
- *"What gear upgrades should I prioritise with 2M GP?"*
- *"Is my current setup good for my Slayer task?"*
- *"What's the fastest way to get from 70 to 80 Slayer?"*

The assistant can see your actual in-game data when answering, so advice is specific to your character rather than generic.

## Available tools

| Tool | Description |
|------|-------------|
| `get_all` | All player data in one call |
| `get_player_stats` | Skill levels, XP, and XP to next level |
| `get_equipment` | Equipped items by slot |
| `get_inventory` | Inventory contents and quantities |
| `get_location` | World coordinates and area name |

---

## Setup

The plugin starts a local MCP server at `http://127.0.0.1:8282/mcp` when RuneLite is running. You then connect your AI tool of choice to that address.

### Claude Desktop (recommended)

1. Download [Claude Desktop](https://claude.ai/download)
2. Open the config file:
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
3. Add the following:

```json
{
  "mcpServers": {
    "osrs": {
      "command": "npx",
      "args": ["mcp-remote", "http://127.0.0.1:8282/mcp"]
    }
  }
}
```

4. Restart Claude Desktop. The OSRS tools will appear automatically.

---

### Cursor

1. Open **Settings → MCP**
2. Add a new server with type `streamable-http` and URL `http://127.0.0.1:8282/mcp`
3. Or add directly to `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "osrs": {
      "url": "http://127.0.0.1:8282/mcp"
    }
  }
}
```

---

### Windsurf

Add to your Windsurf MCP config (`~/.codeium/windsurf/mcp_config.json`):

```json
{
  "mcpServers": {
    "osrs": {
      "serverUrl": "http://127.0.0.1:8282/mcp"
    }
  }
}
```

---

### Any other MCP-compatible tool

If your AI tool supports MCP with a streamable HTTP transport, point it at:

```
http://127.0.0.1:8282/mcp
```

If it only supports stdio transport (not HTTP directly), use the `mcp-remote` bridge:

```bash
npx mcp-remote http://127.0.0.1:8282/mcp
```

---

## LAN / cross-device setup

To use Claude on a laptop while RuneLite runs on a desktop (same network):

1. Enable **Allow LAN connections** in the plugin settings
2. Set an **Auth Token** in the plugin settings (e.g. `my-secret-token`)
3. Find your PC's local IP address (e.g. `192.168.0.10`)
4. On the other device, point your MCP client at `http://192.168.0.10:8282/mcp` with the auth token as a Bearer header

Example for Claude Desktop:

```json
{
  "mcpServers": {
    "osrs": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://192.168.0.10:8282/mcp",
        "--header",
        "Authorization: Bearer my-secret-token"
      ]
    }
  }
}
```

---

## Privacy

- The server only binds to `127.0.0.1` (localhost) by default — never exposed to the internet
- Per-toggle controls for stats, equipment, inventory, location and username in plugin settings
- Optional auth token for shared or LAN setups
- Read-only — the plugin never sends commands to the game

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| Port | 8282 | Port the MCP server listens on |
| Allow LAN connections | Off | Bind to all interfaces for cross-device use |
| Auth Token | (empty) | Optional Bearer token for authentication |
| Share skill levels | On | Allow the AI to read your skills |
| Share equipment | On | Allow the AI to see equipped gear |
| Share inventory | On | Allow the AI to see inventory |
| Share location | On | Allow the AI to see your location |
| Share username | On | Include your RSN in responses |
