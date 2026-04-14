# OSRS MCP

Exposes your RuneLite client data via a local [MCP (Model Context Protocol)](https://modelcontextprotocol.io) server, letting any MCP-compatible AI assistant see your stats, gear, quests, bank, farming patches, drop tables and more to give genuinely useful in-game advice.

> MCP is an open standard supported by Claude, Cursor, Windsurf, and a growing number of AI tools.

## What it does

Ask your AI assistant things like:

- *"What gear upgrades do I have in my bank for Slayer?"*
- *"What's the best money making method I can do right now?"*
- *"Is Cerberus worth farming at my gear level?"*
- *"What should I flip on the GE with 5M GP?"*
- *"Which of my herb patches are ready to harvest?"*
- *"What's my max hit on my current Slayer task?"*
- *"How do I get to Alchemical Hydra with my quest unlocks?"*
- *"Which item in my bank should I sell right now?"*

The plugin answers all of these from live game data and persistent caches — no manual input needed.

---

## Available tools

### Core snapshot

| Tool | Description |
|------|-------------|
| `get_all` | Lightweight snapshot: stats, equipment, inventory, location, quests, diaries, prayers, slayer task, clue scroll, bank summary, GE offers, world info |
| `get_player_stats` | Skill levels, XP, XP to next level, combat level |
| `get_equipment` | Equipped items by slot |
| `get_inventory` | Inventory contents and quantities |
| `get_location` | World coordinates and area name |

### Progression

| Tool | Description |
|------|-------------|
| `get_quest_states` | All quests (completed / in progress / not started) and total quest points |
| `get_diary_states` | All 12 Achievement Diary regions across easy/medium/hard/elite |
| `get_collection_log` | Collection log progress: total unique items and per-category breakdown |
| `get_prayers` | Currently active prayers and unlock status for Preserve, Rigour, Augury, Piety |

### Active content

| Tool | Description |
|------|-------------|
| `get_slayer_task` | Current task, kills remaining, location, points and streak |
| `get_clue_scroll` | Active clue scroll tier from inventory scan |
| `get_ge_offers` | All active Grand Exchange offers |
| `get_nearby_npcs` | NPCs currently visible, sorted by combat level |
| `get_world_info` | World number and type (members, PvP, high risk, deadman, etc.) |

### Bank

| Tool | Description |
|------|-------------|
| `get_bank_summary` | Total value, item count and coin balance |
| `get_bank_top_value` | Top 100 items by total GE value |
| `get_bank_coins` | Coin totals across inventory and bank |
| `get_bank_classified` | Bank split by category: equipment (with slot), food, potions, runes, ammo, materials. Includes Wiki examine text |

### Economy and flipping

| Tool | Description |
|------|-------------|
| `get_item_prices` | Live OSRS Wiki GE prices for specific item IDs |
| `get_flip_suggestions` | Top flip candidates from your bank, sorted by margin × volume, filtered by coin budget |
| `get_money_making_context` | Location, stats, coins and slayer task — all the context needed for GP/hr advice |
| `get_price_trends` | 5m and 1h averages with rising/falling/stable direction per item |

### Combat and gear

| Tool | Description |
|------|-------------|
| `get_equipment_stats` | Full stat bonuses per equipped slot from the Wiki, totals, and estimated base max hit |
| `get_bis_comparison` | Compare current gear against banked items slot-by-slot for melee, ranged or magic. Pass `style` as the argument |
| `get_combat_context` | Attack style, spec energy, active prayers, potion detection from inventory, current target NPC. Falls back to slayer task if not in combat |
| `get_boss_kc` | Boss kill counts — game-tracked (Zulrah, Vorkath, Hydra etc.) plus ChatCommands profile KCs for everything else |
| `get_npc_info` | Monster stats from the Wiki by name: combat level, defence bonuses, max hit, weaknesses, immune to poison/venom |
| `get_drop_table` | Full drop table from the Wiki: always drops, uniques and regulars, each with live GE price and expected GP per kill |

### Farming

| Tool | Description |
|------|-------------|
| `get_farming_patches` | State of all 9 herb patches: growing (with estimated time remaining), harvestable (with live GE price), empty, diseased or dead. Reads from Time Tracking plugin config — works even when not near the patches |
| `get_seed_vault` | Seed vault contents with quantities and live GE prices |

### Meta

| Tool | Description |
|------|-------------|
| `get_installed_plugins` | All installed RuneLite plugins with enabled state |
| `get_cache_index` | Lists all cache files with their last-updated timestamps |
| `read_cache` | Returns the full markdown contents of a cache file (e.g. `bank.md`, `equipment.md`) |

---

## Cache system

The plugin writes human-readable markdown files to `~/.runelite/osrs-mcp/` and keeps them updated automatically:

| File | Updated when |
|------|-------------|
| `character.md` | Any skill XP change (levels, skilling) |
| `equipment.md` | Equipment container changes (gear swaps) |
| `bank.md` | Bank container opens |
| `seed_vault.md` | Seed vault opens |
| `quests.md` | `get_quest_states` is called |
| `farming.md` | `get_farming_patches` is called |

**Why this matters:** bank contents, equipped gear, and character stats persist across sessions. The AI can read your bank data immediately after logging in without you opening the bank first. Bank and seed vault items are also saved to RuneLite's profile config as a fallback.

**Accessing cache files:** If your AI tool has filesystem access (e.g. Desktop Commander), it can read the `.md` files directly from disk. Otherwise, use `get_cache_index` and `read_cache` via MCP — these tools serve the file contents over the MCP connection and work on any setup including Tailscale and Cloud Relay.


---

## Connection modes

Start with the simplest mode that fits your setup.

### Mode 1 — Local (same machine)

The default. RuneLite and your AI tool are on the same machine.

1. Install the plugin from the Plugin Hub
2. Open your Claude Desktop config:
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
3. Add:

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

4. Restart Claude Desktop.

---

### Mode 2 — LAN (same network, different devices)

RuneLite is on one device, your AI tool is on another, both on the same network.

1. Set **Connection mode** to **LAN** in plugin settings and click **Restart server**
2. The panel shows your LAN IP and the correct config snippet — copy it directly
3. Or manually:

```json
{
  "mcpServers": {
    "osrs": {
      "command": "npx",
      "args": ["mcp-remote", "http://192.168.x.x:8282/mcp", "--allow-http"]
    }
  }
}
```

> `--allow-http` is required for non-localhost URLs.

> If devices can't reach each other, they may be on different subnets (e.g. one wired, one wireless through a different router). Use Tailscale instead.

---

### Mode 3 — Tailscale (different networks, recommended)

[Tailscale](https://tailscale.com) is free and gives every device a stable permanent IP regardless of network.

1. Set **Connection mode** to **Tailscale** in plugin settings
2. The panel guides you through setup:
   - If not installed: shows an install link
   - If running: shows your Tailscale IP and config snippet to copy
3. Install Tailscale on both devices, sign in with the same account
4. Click **Restart server**

---

### Mode 4 — Cloud Relay (no extra software)

SSH reverse tunnelling for when you can't install Tailscale. SSH is built into macOS and Windows 10/11.

1. Set **Connection mode** to **Cloud relay** in plugin settings
2. Click **Restart server** — the panel shows a public HTTPS URL once connected
3. Use **Copy config** to paste the snippet into your AI tool config

Uses [serveo.net](https://serveo.net) with automatic fallback to [localhost.run](https://localhost.run). Both are free with no account required.

> The URL changes every restart unless you set up a stable subdomain (see below).

#### Stable URL (optional)

1. Enter a unique subdomain in the **Stable URL setup** field (e.g. `yourname-osrs-mcp`)
2. Click **Copy register URL**, open it in your browser and sign in with Google or GitHub
3. Click **Copy domain URL**, open it, click **Add Domain** and enter your subdomain
4. Click **Save & restart** — your URL will always be `https://yourname-osrs-mcp.serveousercontent.com/mcp`

> Serveo's key authentication can be intermittently unreliable, falling back to a random URL even when correctly configured. Tailscale is the more reliable option if a stable URL is important.

---

## Other AI tools

The plugin works with any MCP-compatible tool.

**Cursor** — add to `~/.cursor/mcp.json`:
```json
{ "mcpServers": { "osrs": { "url": "http://127.0.0.1:8282/mcp" } } }
```

**Windsurf** — add to `~/.codeium/windsurf/mcp_config.json`:
```json
{ "mcpServers": { "osrs": { "serverUrl": "http://127.0.0.1:8282/mcp" } } }
```

**Any other tool** — point it at `http://127.0.0.1:8282/mcp` using streamable HTTP transport, or use `npx mcp-remote http://127.0.0.1:8282/mcp` as a stdio bridge.

---

## Notes on specific tools

**`get_farming_patches`** requires the [Time Tracking](https://github.com/runelite/runelite/wiki/Time-Tracking) plugin to be active. It stores patch states whenever you visit a farming patch — without it, all patches will show as unknown until visited.

**`get_boss_kc`** profile kill counts require the [Chat Commands](https://github.com/runelite/runelite/wiki/Chat-Commands) plugin to be active. Game-tracked KCs (Zulrah, Vorkath, Kraken, Hydra, Grotesque Guardians, Barrows) are always available.

**`get_bis_comparison`** and bank tools require opening your bank at least once per session to populate the cache. After that, data persists across sessions automatically.

**`get_equipment_stats`** and **`get_npc_info`** fetch data from the OSRS Wiki and cache it for 24 hours. The first call for a new item may take a moment.

---

## Privacy

- Binds to `127.0.0.1` by default — never exposed externally unless you choose LAN, Tailscale or Cloud Relay
- Individual toggles for stats, equipment, inventory, location and username in plugin settings
- Optional Bearer token for LAN and relay setups
- Read-only — the plugin never sends commands to the game
- Cloud Relay: your data passes through serveo.net's SSH tunnel. This is opt-in and clearly labelled in the plugin panel

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| Port | 8282 | Port the MCP server listens on |
| Connection mode | Local | Local / LAN / Tailscale / Cloud relay |
| Auth token | (empty) | Optional Bearer token for extra security |
| Share stats | On | Allow the AI to read your skill levels |
| Share equipment | On | Allow the AI to see equipped gear |
| Share inventory | On | Allow the AI to see inventory contents |
| Share location | On | Allow the AI to see your location |
| Share username | On | Include your RSN in responses |
| Stable subdomain | (empty) | Cloud Relay only — subdomain for a permanent URL |
