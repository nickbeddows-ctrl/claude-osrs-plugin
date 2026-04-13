package com.osrsmcp;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches and caches NPC drop tables from the OSRS Wiki.
 * Parses {{DropsLine}} templates from the wiki markup.
 * Cache TTL is 24 hours.
 */
@Slf4j
@Singleton
public class DropTableService
{
    private static final String USER_AGENT = "osrs-mcp-plugin/1.0 (github.com/nickbeddows-ctrl/osrs-mcp-plugin)";
    private static final long   CACHE_TTL  = 24 * 60 * 60 * 1000L;

    @Inject private OkHttpClient httpClient;
    @Inject private WikiPriceService wikiPriceService;

    public static class DropEntry
    {
        public String name;
        public String quantity;
        public String rarity;
        public double rarityDecimal; // 0.0 if unknown/always
        public boolean isAlways;
        public int itemId = -1;
        public int gePrice;
        public double expectedValuePer; // expected GP per kill from this drop
    }

    private final Map<String, CachedDrops> cache = new ConcurrentHashMap<>();

    private static class CachedDrops
    {
        List<DropEntry> drops;
        long fetchedAt;
    }

    public Map<String, Object> getDropTable(String npcName)
    {
        if (npcName == null || npcName.trim().isEmpty())
            return error("No NPC name provided");

        String key = npcName.toLowerCase().trim();
        CachedDrops cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL)
            return buildResult(npcName, cached.drops);

        String pageTitle = toPageTitle(npcName);
        try
        {
            Request req = new Request.Builder()
                .url("https://oldschool.runescape.wiki/w/Special:Export/" + pageTitle)
                .header("User-Agent", USER_AGENT)
                .build();
            String body;
            try (Response resp = httpClient.newCall(req).execute())
            {
                if (!resp.isSuccessful()) return error("Wiki page not found for: " + npcName);
                body = resp.body().string();
            }

            String wikiText = extractWikiText(body);
            if (wikiText == null) return error("Could not parse Wiki for: " + npcName);

            List<DropEntry> drops = parseDrops(wikiText);
            if (drops.isEmpty()) return error("No drop table found for: " + npcName);

            // Enrich with GE prices
            enrichWithPrices(drops);

            CachedDrops cd = new CachedDrops();
            cd.drops = drops; cd.fetchedAt = System.currentTimeMillis();
            cache.put(key, cd);
            return buildResult(npcName, drops);
        }
        catch (Exception e)
        {
            log.warn("OSRS MCP: Drop table fetch failed for {}: {}", npcName, e.getMessage());
            return error("Failed to fetch drop table: " + e.getMessage());
        }
    }

    private List<DropEntry> parseDrops(String wikiText)
    {
        List<DropEntry> drops = new ArrayList<>();
        // Match {{DropsLine|name=X|quantity=Y|rarity=Z|...}}
        Pattern p = Pattern.compile("\\{\\{DropsLine\\|([^\\}]+)\\}\\}");
        Matcher m = p.matcher(wikiText);
        while (m.find())
        {
            String params = m.group(1);
            String name     = param(params, "name");
            String quantity = param(params, "quantity");
            String rarity   = param(params, "rarity");
            if (name == null || name.isEmpty()) continue;

            DropEntry e = new DropEntry();
            e.name     = name;
            e.quantity = quantity != null ? quantity : "1";
            e.rarity   = rarity   != null ? rarity   : "Unknown";
            e.isAlways = "always".equalsIgnoreCase(e.rarity);
            e.rarityDecimal = parseRarity(e.rarity);
            drops.add(e);
        }
        return drops;
    }

    private String param(String params, String key)
    {
        Pattern p = Pattern.compile("(?:^|\\|)" + Pattern.quote(key) + "=([^\\|]+)");
        Matcher m = p.matcher(params);
        if (!m.find()) return null;
        return m.group(1).replaceAll("<[^>]+>", "").replaceAll("\\{\\{[^\\}]+\\}\\}", "").trim();
    }

    private double parseRarity(String rarity)
    {
        if (rarity == null || "always".equalsIgnoreCase(rarity) || "varies".equalsIgnoreCase(rarity)) return 0;
        // Handle N/D format
        Matcher m = Pattern.compile("(\\d+)/(\\d+)").matcher(rarity);
        if (m.find())
        {
            double n = Double.parseDouble(m.group(1));
            double d = Double.parseDouble(m.group(2));
            return d > 0 ? n / d : 0;
        }
        return 0;
    }

    private void enrichWithPrices(List<DropEntry> drops)
    {
        Map<Integer, WikiPriceService.ItemMeta> allMeta = wikiPriceService.getAllMeta();
        // Build name->id lookup
        Map<String, Integer> nameToId = new HashMap<>();
        for (Map.Entry<Integer, WikiPriceService.ItemMeta> entry : allMeta.entrySet())
            if (entry.getValue() != null && entry.getValue().name != null)
                nameToId.put(entry.getValue().name.toLowerCase(), entry.getKey());

        for (DropEntry drop : drops)
        {
            Integer id = nameToId.get(drop.name.toLowerCase());
            if (id == null) continue;
            drop.itemId = id;
            WikiPriceService.PriceData pd = wikiPriceService.getPrice(id);
            if (pd != null && pd.low > 0)
            {
                drop.gePrice = pd.low; // use sell price (conservative)
                // Parse quantity for expected value -- take midpoint of ranges
                double qty = parseQuantity(drop.quantity);
                if (drop.rarityDecimal > 0)
                    drop.expectedValuePer = qty * pd.low * drop.rarityDecimal;
                else if (drop.isAlways)
                    drop.expectedValuePer = qty * pd.low;
            }
        }
    }

    private double parseQuantity(String qty)
    {
        if (qty == null) return 1;
        qty = qty.replaceAll("\\(noted\\)", "").trim();
        Matcher range = Pattern.compile("(\\d+)-(\\d+)").matcher(qty);
        if (range.find())
            return (Double.parseDouble(range.group(1)) + Double.parseDouble(range.group(2))) / 2.0;
        Matcher single = Pattern.compile("(\\d+)").matcher(qty);
        if (single.find()) return Double.parseDouble(single.group(1));
        return 1;
    }

    private Map<String, Object> buildResult(String npcName, List<DropEntry> drops)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("npc", npcName);
        result.put("wiki_url", "https://oldschool.runescape.wiki/w/" + toPageTitle(npcName));

        List<Map<String, Object>> always  = new ArrayList<>();
        List<Map<String, Object>> uniques = new ArrayList<>();
        List<Map<String, Object>> regular = new ArrayList<>();

        double totalExpectedValue = 0;

        for (DropEntry d : drops)
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",     d.name);
            m.put("quantity", d.quantity);
            m.put("rarity",   d.rarity);
            if (d.gePrice > 0)   m.put("ge_price",     d.gePrice);
            if (d.expectedValuePer > 0)
            {
                m.put("expected_gp_per_kill", (long) d.expectedValuePer);
                totalExpectedValue += d.expectedValuePer;
            }

            if (d.isAlways)          always.add(m);
            else if (isUnique(d))    uniques.add(m);
            else                     regular.add(m);
        }

        // Sort uniques and regular by expected value desc
        Comparator<Map<String,Object>> byValue = (a, b) -> {
            long av = a.containsKey("expected_gp_per_kill") ? (long) a.get("expected_gp_per_kill") : 0;
            long bv = b.containsKey("expected_gp_per_kill") ? (long) b.get("expected_gp_per_kill") : 0;
            return Long.compare(bv, av);
        };
        uniques.sort(byValue);
        regular.sort(byValue);

        result.put("expected_gp_per_kill", (long) totalExpectedValue);
        result.put("always_drops",   always);
        result.put("unique_drops",   uniques);
        result.put("regular_drops",  regular);
        return result;
    }

    private boolean isUnique(DropEntry d)
    {
        if (d.rarityDecimal <= 0) return false;
        return d.rarityDecimal < 1.0 / 50; // rarer than 1/50 = treat as unique
    }

    private String toPageTitle(String name)
    {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words)
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append("_");
        String t = sb.toString();
        if (t.endsWith("_")) t = t.substring(0, t.length() - 1);
        try { return java.net.URLEncoder.encode(t, java.nio.charset.StandardCharsets.UTF_8.name()); }
        catch (Exception e) { return t; }
    }

    private String extractWikiText(String xml)
    {
        int s = xml.indexOf("<text"); int e = xml.indexOf("</text>");
        if (s < 0 || e < 0) return null;
        return xml.substring(xml.indexOf(">", s) + 1, e);
    }

    private Map<String, Object> error(String msg)
    { Map<String, Object> m = new LinkedHashMap<>(); m.put("error", msg); return m; }
}
