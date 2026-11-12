package com.thevoos.megatools.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.thevoos.megatools.MegaTools;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tool aliases: lets a stored tool be pre-selected for blocks from OTHER mods.
 *
 * File: config/megatools-tool-aliases.json (created with an explanation on first launch).
 *
 * {
 *   "items": {                                         // one specific tool -> extra mod ids
 *     "modern_industrialization:wrench": ["mekanism", "create"]
 *   },
 *   "mods": {                                          // every tool from a mod -> extra mod ids
 *     "integrateddynamics": ["integratedterminals", "integratedtunnels"]
 *   }
 * }
 *
 * A value may be a single string or a list. Listing "minecraft" makes the tool match vanilla
 * blocks even when autoSwapIgnoreVanilla is on. Auto-swap pre-selection runs on the client, so
 * this file is read by the client (put it in the modpack's config folder).
 * It is re-read every time you join a world or server, so edits apply without restarting.
 */
public final class ToolAliases
{
    private static final String FILE_NAME = "megatools-tool-aliases.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Map<ResourceLocation, Set<String>> itemAliases = Map.of();
    private static Map<String, Set<String>> modAliases = Map.of();
    private static boolean loaded = false;

    private ToolAliases() {}

    /** True if the tool with this registry id should be offered for blocks from {@code blockMod}. */
    public static boolean matches(ResourceLocation toolId, String blockMod)
    {
        if (!loaded)
            reload();
        Set<String> forItem = itemAliases.get(toolId);
        if (forItem != null && forItem.contains(blockMod))
            return true;
        Set<String> forMod = modAliases.get(toolId.getNamespace());
        return forMod != null && forMod.contains(blockMod);
    }

    /** (Re)reads the file. Creates it with an explanation if it doesn't exist. Never throws. */
    public static void reload()
    {
        loaded = true;
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try
        {
            if (Files.notExists(path))
                writeDefault(path);

            JsonObject root;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
            {
                JsonReader json = new JsonReader(reader);
                json.setLenient(true); // tolerate // comments and trailing commas typed by hand
                root = JsonParser.parseReader(json).getAsJsonObject();
            }

            Map<ResourceLocation, Set<String>> items = new HashMap<>();
            if (root.has("items") && root.get("items").isJsonObject())
            {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("items").entrySet())
                {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey().trim().toLowerCase(Locale.ROOT));
                    if (id == null)
                        MegaTools.LOGGER.warn("[{}] ignoring invalid item id '{}'", FILE_NAME, entry.getKey());
                    else
                        items.computeIfAbsent(id, k -> new HashSet<>()).addAll(readModList(entry.getValue()));
                }
            }

            Map<String, Set<String>> mods = new HashMap<>();
            if (root.has("mods") && root.get("mods").isJsonObject())
            {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("mods").entrySet())
                    mods.computeIfAbsent(entry.getKey().trim().toLowerCase(Locale.ROOT), k -> new HashSet<>())
                        .addAll(readModList(entry.getValue()));
            }

            itemAliases = items;
            modAliases = mods;
            MegaTools.LOGGER.info("[{}] loaded {} item alias(es) and {} mod alias(es)", FILE_NAME, items.size(), mods.size());
        }
        catch (Exception e)
        {
            // A typo in the file must never crash the game: keep running without aliases.
            itemAliases = Map.of();
            modAliases = Map.of();
            MegaTools.LOGGER.error("[{}] could not be read, tool aliases disabled until it is fixed", FILE_NAME, e);
        }
    }

    /** Accepts "modid" or ["modid1", "modid2"]. */
    private static Set<String> readModList(JsonElement value)
    {
        Set<String> result = new HashSet<>();
        if (value.isJsonPrimitive())
            result.add(value.getAsString().trim().toLowerCase(Locale.ROOT));
        else if (value.isJsonArray())
            for (JsonElement element : value.getAsJsonArray())
                result.add(element.getAsString().trim().toLowerCase(Locale.ROOT));
        result.removeIf(String::isEmpty);
        return result;
    }

    private static void writeDefault(Path path) throws Exception
    {
        JsonObject root = new JsonObject();
        root.addProperty("_help",
                "Tool auto-swap aliases. 'items': a tool's item id -> mod ids whose blocks it should be pre-selected for. "
              + "'mods': every tool from a mod -> extra mod ids. Values can be a string or a list. "
              + "Use F3+H in game to see item ids. Applied when you join a world.");

        JsonObject exampleItems = new JsonObject();
        JsonArray wrenchMods = new JsonArray();
        wrenchMods.add("mekanism");
        exampleItems.add("modern_industrialization:wrench", wrenchMods);
        root.add("_example_items", exampleItems);

        root.add("items", new JsonObject());
        root.add("mods", new JsonObject());

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            GSON.toJson(root, writer);
        }
    }
}
