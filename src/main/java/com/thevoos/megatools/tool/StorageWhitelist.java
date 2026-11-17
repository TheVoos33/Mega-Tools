package com.thevoos.megatools.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.items.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Storage whitelist: gives the item tag #megatools:storable_item to items that have no suitable tag
 * (e.g. SFM's Label Gun, Construction Sticks, Building Gadgets) and to every item of the listed tags.
 * Tool storage only accepts items that have #megatools:storable_item, so this file never allows an
 * item directly: it only adds entries to that tag.
 *
 * File: config/megatools-storage-whitelist.json (created with defaults on first launch).
 *
 * {
 *   "version": 2,
 *   "tags": ["forge:tools/wrench"],                   // every item with one of these tags gets the tag
 *   "items": {
 *     "sfm": ["sfm:labelgun", "sfm:network_tool"],    // mod id -> items (or "#tags") that get the tag
 *     "constructionstick": ["constructionstick:iron_stick"]
 *   }
 * }
 *
 * A group under "items" is only applied if its mod id is loaded, so one file can list tools for mods
 * that a modpack doesn't have. Ids and tags that don't exist are skipped (optional tag entries).
 *
 * The file is read every time item tags are loaded (world load and /reload, see TagLoaderMixin) and its
 * entries are put in the tag in front of the datapacks' entries, so a datapack can take any of them out
 * again with a "remove" list. Tags are synced to clients by vanilla, so the
 * server's file decides and nothing else has to be sent. In singleplayer that's the client's own
 * config folder.
 */
public final class StorageWhitelist
{
    public static final String FILE_NAME = "megatools-storage-whitelist.json";
    /** Format of the file. 1 (no "version" key): Mega Tools 1.0, a plain whitelist without "tags". */
    private static final int VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private StorageWhitelist() {}

    /**
     * (Re)reads the file, creating it with defaults if missing, and returns the entries to add to
     * #megatools:storable_item. Never throws: a broken file gives no entries.
     */
    public static List<TagEntry> loadTagEntries()
    {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        Set<ResourceLocation> tagIds = new LinkedHashSet<>();
        int skippedMods = 0;
        try
        {
            if (Files.notExists(path))
                write(path, defaultFile());

            JsonObject root;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
            {
                JsonReader json = new JsonReader(reader);
                json.setLenient(true); // tolerate // comments typed by hand
                root = JsonParser.parseReader(json).getAsJsonObject();
            }

            if (version(root) < VERSION)
            {
                upgrade(root);
                write(path, root);
                MegaTools.LOGGER.info("[{}] updated to format {}: new default tools and the \"tags\" list were added",
                        FILE_NAME, VERSION);
            }

            if (root.has("tags"))
                for (String entry : readList(root.get("tags")))
                    addEntry(entry, true, itemIds, tagIds);

            if (root.has("items") && root.get("items").isJsonObject())
            {
                for (Map.Entry<String, JsonElement> group : root.getAsJsonObject("items").entrySet())
                {
                    String modId = group.getKey().trim().toLowerCase(Locale.ROOT);
                    if (!ModList.get().isLoaded(modId))
                    {
                        skippedMods++;
                        continue;
                    }
                    for (String entry : readList(group.getValue()))
                        addEntry(entry, false, itemIds, tagIds);
                }
            }
            MegaTools.LOGGER.info("[{}] {} item(s) and {} tag(s) added to #{} ({} mod group(s) skipped, mod not loaded)",
                    FILE_NAME, itemIds.size(), tagIds.size(), ModTags.STORABLE_ITEM.location(), skippedMods);
        }
        catch (Exception e)
        {
            // A typo in the file must never crash the game: run with the datapack tag only until it is fixed.
            itemIds.clear();
            tagIds.clear();
            MegaTools.LOGGER.error("[{}] could not be read, nothing is added to #{} until it is fixed",
                    FILE_NAME, ModTags.STORABLE_ITEM.location(), e);
        }

        // Optional entries: an item or tag that doesn't exist is skipped instead of breaking the whole tag.
        List<TagEntry> entries = new ArrayList<>();
        tagIds.forEach(id -> entries.add(TagEntry.optionalTag(id)));
        itemIds.forEach(id -> entries.add(TagEntry.optionalElement(id)));
        return entries;
    }

    /** One entry of the file: an item id, or a tag ("#id", or a plain id in the "tags" list). */
    private static void addEntry(String entry, boolean tagList, Set<ResourceLocation> itemIds, Set<ResourceLocation> tagIds)
    {
        boolean isTag = tagList || entry.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(entry.startsWith("#") ? entry.substring(1) : entry);
        if (id == null)
            MegaTools.LOGGER.warn("[{}] ignoring invalid id '{}'", FILE_NAME, entry);
        else if (!isTag)
            itemIds.add(id);
        else if (!id.equals(ModTags.STORABLE_ITEM.location())) // the tag can't contain itself
            tagIds.add(id);
    }

    private static int version(JsonObject root)
    {
        try
        {
            return root.has("version") ? root.get("version").getAsInt() : 1;
        }
        catch (RuntimeException e)
        {
            return 1;
        }
    }

    /**
     * One-time update of a file written by an older version: adds the "tags" list and the default
     * tools that are new since then. Everything already in the file is kept.
     */
    private static void upgrade(JsonObject root)
    {
        JsonObject defaults = defaultFile();
        root.add("_help", defaults.get("_help"));
        root.addProperty("version", VERSION);
        if (!root.has("tags"))
            root.add("tags", defaults.get("tags"));

        if (!root.has("items") || !root.get("items").isJsonObject())
            root.add("items", new JsonObject());
        JsonObject groups = root.getAsJsonObject("items");
        for (Map.Entry<String, JsonElement> group : defaults.getAsJsonObject("items").entrySet())
        {
            if (!groups.has(group.getKey()))
            {
                groups.add(group.getKey(), group.getValue());
                continue;
            }
            List<String> existing = readList(groups.get(group.getKey()));
            JsonArray merged = new JsonArray();
            existing.forEach(merged::add);
            for (String id : readList(group.getValue()))
                if (!existing.contains(id))
                    merged.add(id);
            groups.add(group.getKey(), merged);
        }
    }

    /** Accepts "id" or ["id1", "id2"]. */
    private static List<String> readList(JsonElement value)
    {
        List<String> result = new ArrayList<>();
        if (value.isJsonPrimitive())
            result.add(value.getAsString());
        else if (value.isJsonArray())
            for (JsonElement element : value.getAsJsonArray())
                result.add(element.getAsString());
        result.replaceAll(s -> s.trim().toLowerCase(Locale.ROOT));
        result.removeIf(String::isEmpty);
        return result;
    }

    private static JsonObject defaultFile()
    {
        JsonObject root = new JsonObject();
        root.addProperty("_help",
                "Tool storage only accepts items that have the item tag #megatools:storable_item. This file gives that "
              + "tag to more items. \"tags\": every item that has one of these item tags also gets the tag. \"items\": "
              + "item ids (or #item_tags) that get the tag, grouped by mod id; a group is only used if that mod is "
              + "installed. Use F3+H in game to see item ids. Re-read on /reload.");
        root.addProperty("version", VERSION);

        JsonArray tags = new JsonArray();
        DEFAULT_TAGS.forEach(tags::add);
        root.add("tags", tags);

        JsonObject groups = new JsonObject();
        DEFAULT_ITEMS.forEach((mod, ids) ->
        {
            JsonArray array = new JsonArray();
            ids.forEach(array::add);
            groups.add(mod, array);
        });
        root.add("items", groups);
        return root;
    }

    private static void write(Path path, JsonObject root) throws Exception
    {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            GSON.toJson(root, writer);
        }
    }

    /** Item tags whose items all get #megatools:storable_item. Written to the file on first launch. */
    private static final List<String> DEFAULT_TAGS = List.of("forge:tools/wrench");

    /** Tools without a suitable tag (checked against All the Mods 10). Written to the file on first launch. */
    private static final Map<String, List<String>> DEFAULT_ITEMS = new LinkedHashMap<>();
    static
    {
        DEFAULT_ITEMS.put("minecraft", List.of("minecraft:flint_and_steel")); // "minecraft" always counts as installed
        DEFAULT_ITEMS.put("sfm", List.of("sfm:labelgun", "sfm:network_tool"));
        DEFAULT_ITEMS.put("constructionstick", List.of(
                "constructionstick:wooden_stick", "constructionstick:copper_stick", "constructionstick:iron_stick",
                "constructionstick:diamond_stick", "constructionstick:netherite_stick"));
        DEFAULT_ITEMS.put("buildinggadgets2", List.of(
                "buildinggadgets2:gadget_building", "buildinggadgets2:gadget_exchanging", "buildinggadgets2:gadget_copy_paste",
                "buildinggadgets2:gadget_cut_paste", "buildinggadgets2:gadget_destruction"));
        DEFAULT_ITEMS.put("mininggadgets", List.of(
                "mininggadgets:mininggadget_simple", "mininggadgets:mininggadget_fancy", "mininggadgets:mininggadget"));
        DEFAULT_ITEMS.put("ae2", List.of("ae2:memory_card"));
        DEFAULT_ITEMS.put("ae2netanalyser", List.of("ae2netanalyser:network_analyser"));
        DEFAULT_ITEMS.put("extendedae", List.of("extendedae:wireless_tool"));
        DEFAULT_ITEMS.put("advancedperipherals", List.of("advancedperipherals:computer_tool", "advancedperipherals:memory_card"));
        DEFAULT_ITEMS.put("integrateddynamics", List.of("integrateddynamics:labeller"));
        DEFAULT_ITEMS.put("functionalstorage", List.of("functionalstorage:configuration_tool", "functionalstorage:linking_tool"));
        DEFAULT_ITEMS.put("sophisticatedstorage", List.of("sophisticatedstorage:storage_tool"));
        DEFAULT_ITEMS.put("draconicevolution", List.of("draconicevolution:crystal_binder"));
        DEFAULT_ITEMS.put("fluxnetworks", List.of("fluxnetworks:flux_configurator"));
        DEFAULT_ITEMS.put("framedblocks", List.of("framedblocks:framed_hammer", "framedblocks:framed_screwdriver", "framedblocks:framed_key"));
        DEFAULT_ITEMS.put("immersiveengineering", List.of("immersiveengineering:wirecutter", "immersiveengineering:voltmeter"));
        DEFAULT_ITEMS.put("pneumaticcraft", List.of(
                "pneumaticcraft:manometer", "pneumaticcraft:logistics_configurator", "pneumaticcraft:gps_tool",
                "pneumaticcraft:gps_area_tool"));
        DEFAULT_ITEMS.put("enderio", List.of("enderio:conduit_probe"));
        DEFAULT_ITEMS.put("pipez", List.of("pipez:filter_destination_tool"));
        DEFAULT_ITEMS.put("actuallyadditions", List.of("actuallyadditions:laser_wrench"));
        DEFAULT_ITEMS.put("ars_nouveau", List.of("ars_nouveau:dominion_wand"));
        DEFAULT_ITEMS.put("ars_additions", List.of("ars_additions:advanced_dominion_wand"));
        DEFAULT_ITEMS.put("railcraft", List.of("railcraft:signal_tuner", "railcraft:whistle_tuner"));
        DEFAULT_ITEMS.put("oritech", List.of("oritech:target_designator"));
        DEFAULT_ITEMS.put("measurements", List.of("measurements:tape_measure"));
        DEFAULT_ITEMS.put("bibliocraft", List.of("bibliocraft:tape_measure"));
        DEFAULT_ITEMS.put("handcrafted", List.of("handcrafted:hammer"));
        DEFAULT_ITEMS.put("mcwwindows", List.of("mcwwindows:hammer"));
        DEFAULT_ITEMS.put("mcwroofs", List.of("mcwroofs:roofing_hammer"));
        DEFAULT_ITEMS.put("minecolonies", List.of(
                "minecolonies:assistanthammer_iron", "minecolonies:assistanthammer_gold", "minecolonies:assistanthammer_diamond"));
        DEFAULT_ITEMS.put("botania", List.of("botania:twig_wand"));
        DEFAULT_ITEMS.put("logisticsnetworks", List.of("logisticsnetworks:wrench"));
        DEFAULT_ITEMS.put("transfer_labels", List.of("transfer_labels:label_accessor"));
        DEFAULT_ITEMS.put("industrialforegoing", List.of("industrialforegoing:machine_settings_copier"));
        DEFAULT_ITEMS.put("justdirethings", List.of("justdirethings:machinesettingscopier"));
        DEFAULT_ITEMS.put("mekanism", List.of("mekanism:configuration_card"));
        DEFAULT_ITEMS.put("hostilenetworks", List.of("hostilenetworks:fab_directive"));
        DEFAULT_ITEMS.put("crystalix", List.of("crystalix:crystalix_wand"));
        DEFAULT_ITEMS.put("rechiseled", List.of("rechiseled:chisel"));
        DEFAULT_ITEMS.put("chisel", List.of("chisel:chisel")); // Chisel Reborn
        DEFAULT_ITEMS.put("chipped", List.of("chipped:chisel"));
    }
}
