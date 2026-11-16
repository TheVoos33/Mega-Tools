package com.thevoos.megatools.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
 *   },
 *   "groups": {                                        // mod families: every tool of any member
 *     "create": ["create", "createaddition", ...]      // works on blocks of every other member
 *   },
 *   "attachments": ["logisticsnetworks:logistics_node"] // entity types that sit ON a block (see BlockAttachments)
 * }
 *
 * A value may be a single string or a list. "minecraft" has no effect: vanilla blocks never
 * pre-select a tool (see AutoSwapClient). Auto-swap pre-selection runs on the client, so
 * this file is read by the client (put it in the modpack's config folder).
 * It is re-read every time you join a world or server, so edits apply without restarting.
 */
public final class ToolAliases
{
    private static final String FILE_NAME = "megatools-tool-aliases.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Map<ResourceLocation, Set<String>> itemAliases = Map.of();
    private static Map<String, Set<String>> modAliases = Map.of();
    private static Set<EntityType<?>> attachmentEntities = Set.of();
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

    /**
     * Entity types that other mods attach to a block (e.g. Logistics Network's node): while one sits on
     * the targeted block, the tools of the entity's mod are pre-selected too (see BlockAttachments).
     */
    public static Set<EntityType<?>> attachmentEntities()
    {
        if (!loaded)
            reload();
        return attachmentEntities;
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

            // A group is a shortcut for "every member's tools also work on every other member's blocks".
            int groups = 0;
            if (root.has("groups") && root.get("groups").isJsonObject())
            {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("groups").entrySet())
                {
                    Set<String> members = readModList(entry.getValue());
                    for (String member : members)
                        mods.computeIfAbsent(member, k -> new HashSet<>()).addAll(members);
                    groups++;
                }
            }

            // Files written before this key existed don't have it: use the defaults then.
            Set<String> attachmentIds = root.has("attachments")
                    ? readModList(root.get("attachments")) : new HashSet<>(DEFAULT_ATTACHMENTS);
            Set<EntityType<?>> attachments = new HashSet<>();
            for (String entry : attachmentIds)
            {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id == null)
                    MegaTools.LOGGER.warn("[{}] ignoring invalid entity id '{}'", FILE_NAME, entry);
                else // not registered = that mod isn't installed
                    BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(attachments::add);
            }

            itemAliases = items;
            modAliases = mods;
            attachmentEntities = Set.copyOf(attachments);
            MegaTools.LOGGER.info("[{}] loaded {} item alias(es), {} mod alias(es), {} group(s) and {} attachment entity type(s)",
                    FILE_NAME, items.size(), mods.size(), groups, attachments.size());
        }
        catch (Exception e)
        {
            // A typo in the file must never crash the game: keep running without aliases.
            itemAliases = Map.of();
            modAliases = Map.of();
            attachmentEntities = Set.of();
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
              + "'mods': every tool from a mod -> extra mod ids. 'groups': mod families; a tool from any mod in a group "
              + "is pre-selected for blocks of every mod in that group (the group name is only a label). "
              + "'attachments': entity types that other mods attach to a block (like Logistics Network's node); while one "
              + "sits on the block you look at, the tools of its mod are pre-selected as well. "
              + "Values can be a string or a list. Mods that aren't installed are ignored. "
              + "Use F3+H in game to see item ids. Applied when you join a world.");

        JsonObject exampleItems = new JsonObject();
        JsonArray wrenchMods = new JsonArray();
        wrenchMods.add("mekanism");
        exampleItems.add("modern_industrialization:wrench", wrenchMods);
        root.add("_example_items", exampleItems);

        root.add("items", new JsonObject());
        root.add("mods", new JsonObject());

        JsonObject groups = new JsonObject();
        DEFAULT_GROUPS.forEach((name, members) ->
        {
            JsonArray array = new JsonArray();
            members.forEach(array::add);
            groups.add(name, array);
        });
        root.add("groups", groups);

        JsonArray attachments = new JsonArray();
        DEFAULT_ATTACHMENTS.forEach(attachments::add);
        root.add("attachments", attachments);

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            GSON.toJson(root, writer);
        }
    }

    /** Entities that are attached to a block and are worked on with their own mod's tools. */
    private static final List<String> DEFAULT_ATTACHMENTS = List.of("logisticsnetworks:logistics_node");

    /**
     * Mod families whose tools are used on each other's blocks (addons and their base mod).
     * Written to the file on first launch. Based on All the Mods 10 plus common addons from other packs
     * and Morph-o-Tool's alias list.
     */
    private static final Map<String, List<String>> DEFAULT_GROUPS = new LinkedHashMap<>();
    static
    {
        DEFAULT_GROUPS.put("applied_energistics", List.of(
                "ae2", "ae2wtlib", "advanced_ae", "extendedae", "expandedae", "megacells", "appflux", "appmek",
                "arseng", "merequester", "ae2ct", "ae2importexportcard", "ae2netanalyser", "aeinfinitybooster",
                "polyeng", "soulplied_energistics", "enderdrives", "immeng", "ae2things", "appbot"));
        DEFAULT_GROUPS.put("create", List.of(
                "create", "createaddition", "create_enchantment_industry", "create_aquatic_ambitions",
                "create_dragons_plus", "create_hypertube", "rechiseledcreate", "bellsandwhistles",
                "sophisticatedbackpackscreateintegration", "sophisticatedstoragecreateintegration",
                "createdeco", "create_connected", "railways", "create_new_age", "createbigcannons", "create_sa",
                "create_jetpack", "createoreexcavation", "create_central_kitchen", "copycats"));
        DEFAULT_GROUPS.put("mekanism", List.of(
                "mekanism", "mekanismgenerators", "mekanismtools", "mekanismcovers", "evolvedmekanism", "mekmm",
                "mekanisticrouters", "mekanism_extras", "mekaturbines"));
        DEFAULT_GROUPS.put("refined_storage", List.of(
                "refinedstorage", "cabletiers", "extrastorage", "extradisks", "refinedtypes", "universalgrid",
                "refinedstorage_mekanism_integration", "refinedstorage_quartz_arsenal", "refinedstorage_curios_integration"));
        DEFAULT_GROUPS.put("rftools", List.of(
                "rftoolsbase", "rftoolsbuilder", "rftoolspower", "rftoolsstorage", "rftoolsutility", "rftoolscontrol",
                "rftoolsdim", "xnet", "deepresonance"));
        DEFAULT_GROUPS.put("integrated_dynamics", List.of(
                "integrateddynamics", "integratedterminals", "integratedtunnels", "integratedcrafting",
                "integratedscripting", "integrateddynamicscompat", "integratedterminalscompat", "integratedtunnelscompat"));
        DEFAULT_GROUPS.put("ender_io", List.of(
                "enderio", "enderio_machines", "enderio_conduits", "enderio_conduits_modded", "enderio_modded_conduits",
                "enderio_armory"));
        DEFAULT_GROUPS.put("industrial_foregoing", List.of("industrialforegoing", "industrialforegoingsouls"));
        DEFAULT_GROUPS.put("modern_industrialization", List.of(
                "modern_industrialization", "extended_industrialization", "industrialization_overdrive"));
        DEFAULT_GROUPS.put("immersive_engineering", List.of(
                "immersiveengineering", "immersivepetroleum", "immersiveposts", "immeng"));
        DEFAULT_GROUPS.put("thermal", List.of(
                "thermal", "thermal_expansion", "thermal_dynamics", "thermal_innovation", "thermal_integration",
                "thermal_cultivation", "thermal_locomotion", "redstone_arsenal"));
        DEFAULT_GROUPS.put("ars_nouveau", List.of(
                "ars_nouveau", "ars_additions", "ars_controle", "ars_creo", "ars_elemancy", "ars_elemental",
                "ars_ocultas", "ars_polymorphia", "ars_technica", "ars_unification", "not_enough_glyphs",
                "starbunclemania", "toomanyglyphs", "arseng"));
        DEFAULT_GROUPS.put("mystical_agriculture", List.of("mysticalagriculture", "mysticalagradditions"));
        DEFAULT_GROUPS.put("silent_gear", List.of("silentgear", "silentgems", "sgearmetalworks"));
        DEFAULT_GROUPS.put("productive_bees", List.of("productivebees", "productivetrees", "productivemetalworks"));
        DEFAULT_GROUPS.put("sophisticated", List.of(
                "sophisticatedstorage", "sophisticatedbackpacks", "sophisticatedcore", "sophisticatedstorageinmotion",
                "sophisticatedbackpackscreateintegration", "sophisticatedstoragecreateintegration"));
        DEFAULT_GROUPS.put("xycraft", List.of("xycraft_core", "xycraft_machines", "xycraft_world", "xycraft_override"));
        DEFAULT_GROUPS.put("draconic_evolution", List.of("draconicevolution", "brandonscore"));
        DEFAULT_GROUPS.put("extreme_reactors", List.of("bigreactors", "zerocore"));
        DEFAULT_GROUPS.put("botany_pots", List.of("botanypots", "botanytrees", "botanypotsmystical"));
        DEFAULT_GROUPS.put("computercraft", List.of("computercraft", "advancedperipherals"));
        DEFAULT_GROUPS.put("modular_routers", List.of("modularrouters", "mekanisticrouters"));
        DEFAULT_GROUPS.put("minecolonies", List.of("minecolonies", "structurize", "domum_ornamentum"));
        DEFAULT_GROUPS.put("evilcraft", List.of("evilcraft", "evilcraftcompat"));
        DEFAULT_GROUPS.put("macaws", List.of(
                "mcwbridges", "mcwdoors", "mcwfences", "mcwfurnitures", "mcwholidays", "mcwlights", "mcwpaths",
                "mcwroofs", "mcwstairs", "mcwtrpdoors", "mcwwindows"));
    }
}
