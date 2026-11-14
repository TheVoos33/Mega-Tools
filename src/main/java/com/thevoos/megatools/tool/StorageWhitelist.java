package com.thevoos.megatools.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.network.StorageWhitelistPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Storage whitelist: items from other mods that may go in tool storage although they have no
 * suitable tag (e.g. SFM's Label Gun, Construction Sticks, Building Gadgets).
 *
 * File: config/megatools-storage-whitelist.json (created with defaults on first launch).
 *
 * {
 *   "items": {
 *     "sfm": ["sfm:labelgun", "sfm:network_tool"],    // mod id -> items (or "#tags") to allow
 *     "constructionstick": ["constructionstick:iron_stick"]
 *   }
 * }
 *
 * A group is only applied if its mod id is loaded, so one file can list tools for mods that
 * a modpack doesn't have. Ids that don't exist are skipped too.
 *
 * The server is authoritative: it reads the file when it starts and on /reload, and sends the
 * result to every client (players joining get it too), so the client's slot checks match the
 * server's. In singleplayer that's the client's own config folder.
 */
public final class StorageWhitelist
{
    private static final String FILE_NAME = "megatools-storage-whitelist.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Immutable snapshots, replaced as a whole (read from the server and client threads).
    private static volatile Set<Item> items = Set.of();
    private static volatile List<TagKey<Item>> tags = List.of();
    // What was loaded/received, in id form, for sending to clients.
    private static volatile List<ResourceLocation> itemIds = List.of();
    private static volatile List<ResourceLocation> tagIds = List.of();

    private StorageWhitelist() {}

    /** True if the file (as loaded by the server) allows this item in tool storage. */
    public static boolean contains(ItemStack stack)
    {
        if (items.contains(stack.getItem()))
            return true;
        for (TagKey<Item> tag : tags)
            if (stack.is(tag))
                return true;
        return false;
    }

    // ---------------------------------------------------------------------------------
    //  Server side
    // ---------------------------------------------------------------------------------

    /** Server: read the file before the world loads. */
    public static void onServerAboutToStart(ServerAboutToStartEvent event)
    {
        reload();
    }

    /**
     * Fired for one player when they join, and for everyone (player == null) after /reload.
     * Re-reads the file on /reload, then sends the current list.
     */
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() == null)
            reload();
        StorageWhitelistPayload payload = new StorageWhitelistPayload(itemIds, tagIds);
        event.getRelevantPlayers().forEach((ServerPlayer player) -> PacketDistributor.sendToPlayer(player, payload));
    }

    /** Client: the server's list. */
    public static void apply(List<ResourceLocation> newItems, List<ResourceLocation> newTags)
    {
        Set<Item> resolved = new HashSet<>();
        for (ResourceLocation id : newItems)
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(resolved::add);
        List<TagKey<Item>> resolvedTags = new ArrayList<>();
        for (ResourceLocation id : newTags)
            resolvedTags.add(TagKey.create(Registries.ITEM, id));

        itemIds = List.copyOf(newItems);
        tagIds = List.copyOf(newTags);
        items = Set.copyOf(resolved);
        tags = List.copyOf(resolvedTags);
    }

    /** (Re)reads the file, creating it with defaults if missing. Never throws. */
    public static void reload()
    {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        List<ResourceLocation> loadedItems = new ArrayList<>();
        List<ResourceLocation> loadedTags = new ArrayList<>();
        int skippedMods = 0;
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
                    {
                        boolean isTag = entry.startsWith("#");
                        ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
                        if (id == null)
                            MegaTools.LOGGER.warn("[{}] ignoring invalid id '{}'", FILE_NAME, entry);
                        else if (isTag)
                            loadedTags.add(id);
                        else if (BuiltInRegistries.ITEM.containsKey(id))
                            loadedItems.add(id);
                        else
                            MegaTools.LOGGER.debug("[{}] item '{}' doesn't exist, skipped", FILE_NAME, id);
                    }
                }
            }
            MegaTools.LOGGER.info("[{}] {} item(s) and {} tag(s) allowed in tool storage ({} mod group(s) skipped, mod not loaded)",
                    FILE_NAME, loadedItems.size(), loadedTags.size(), skippedMods);
        }
        catch (Exception e)
        {
            // A typo in the file must never crash the game: run with tags only until it is fixed.
            loadedItems.clear();
            loadedTags.clear();
            MegaTools.LOGGER.error("[{}] could not be read, storage whitelist disabled until it is fixed", FILE_NAME, e);
        }
        apply(loadedItems, loadedTags);
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

    private static void writeDefault(Path path) throws Exception
    {
        JsonObject root = new JsonObject();
        root.addProperty("_help",
                "Extra items allowed in Mega Tools tool storage, grouped by mod id. A group is only used if that mod is "
              + "installed. Values are item ids or #item_tags. Items in #c:tools/wrench or #megatools:storable_item are "
              + "always allowed and don't need to be listed. Use F3+H in game to see item ids. Re-read on /reload.");

        JsonObject groups = new JsonObject();
        DEFAULTS.forEach((mod, ids) ->
        {
            JsonArray array = new JsonArray();
            ids.forEach(array::add);
            groups.add(mod, array);
        });
        root.add("items", groups);

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            GSON.toJson(root, writer);
        }
    }

    /** Tools without a suitable tag (checked against All the Mods 10). Written to the file on first launch. */
    private static final Map<String, List<String>> DEFAULTS = new LinkedHashMap<>();
    static
    {
        DEFAULTS.put("minecraft", List.of("minecraft:flint_and_steel")); // "minecraft" always counts as installed
        DEFAULTS.put("sfm", List.of("sfm:labelgun", "sfm:network_tool"));
        DEFAULTS.put("constructionstick", List.of(
                "constructionstick:wooden_stick", "constructionstick:copper_stick", "constructionstick:iron_stick",
                "constructionstick:diamond_stick", "constructionstick:netherite_stick"));
        DEFAULTS.put("buildinggadgets2", List.of(
                "buildinggadgets2:gadget_building", "buildinggadgets2:gadget_exchanging", "buildinggadgets2:gadget_copy_paste",
                "buildinggadgets2:gadget_cut_paste", "buildinggadgets2:gadget_destruction"));
        DEFAULTS.put("mininggadgets", List.of(
                "mininggadgets:mininggadget_simple", "mininggadgets:mininggadget_fancy", "mininggadgets:mininggadget"));
        DEFAULTS.put("ae2", List.of("ae2:memory_card"));
        DEFAULTS.put("ae2netanalyser", List.of("ae2netanalyser:network_analyser"));
        DEFAULTS.put("extendedae", List.of("extendedae:wireless_tool"));
        DEFAULTS.put("advancedperipherals", List.of("advancedperipherals:computer_tool", "advancedperipherals:memory_card"));
        DEFAULTS.put("integrateddynamics", List.of("integrateddynamics:labeller"));
        DEFAULTS.put("functionalstorage", List.of("functionalstorage:configuration_tool", "functionalstorage:linking_tool"));
        DEFAULTS.put("sophisticatedstorage", List.of("sophisticatedstorage:storage_tool"));
        DEFAULTS.put("draconicevolution", List.of("draconicevolution:crystal_binder"));
        DEFAULTS.put("fluxnetworks", List.of("fluxnetworks:flux_configurator"));
        DEFAULTS.put("framedblocks", List.of("framedblocks:framed_hammer", "framedblocks:framed_screwdriver", "framedblocks:framed_key"));
        DEFAULTS.put("immersiveengineering", List.of("immersiveengineering:wirecutter", "immersiveengineering:voltmeter"));
        DEFAULTS.put("pneumaticcraft", List.of(
                "pneumaticcraft:manometer", "pneumaticcraft:logistics_configurator", "pneumaticcraft:gps_tool",
                "pneumaticcraft:gps_area_tool"));
        DEFAULTS.put("enderio", List.of("enderio:conduit_probe"));
        DEFAULTS.put("pipez", List.of("pipez:filter_destination_tool"));
        DEFAULTS.put("actuallyadditions", List.of("actuallyadditions:laser_wrench"));
        DEFAULTS.put("ars_nouveau", List.of("ars_nouveau:dominion_wand"));
        DEFAULTS.put("ars_additions", List.of("ars_additions:advanced_dominion_wand"));
        DEFAULTS.put("railcraft", List.of("railcraft:signal_tuner", "railcraft:whistle_tuner"));
        DEFAULTS.put("oritech", List.of("oritech:target_designator"));
        DEFAULTS.put("measurements", List.of("measurements:tape_measure"));
        DEFAULTS.put("bibliocraft", List.of("bibliocraft:tape_measure"));
        DEFAULTS.put("handcrafted", List.of("handcrafted:hammer"));
        DEFAULTS.put("mcwwindows", List.of("mcwwindows:hammer"));
        DEFAULTS.put("mcwroofs", List.of("mcwroofs:roofing_hammer"));
        DEFAULTS.put("minecolonies", List.of(
                "minecolonies:assistanthammer_iron", "minecolonies:assistanthammer_gold", "minecolonies:assistanthammer_diamond"));
        DEFAULTS.put("botania", List.of("botania:twig_wand"));
    }
}
