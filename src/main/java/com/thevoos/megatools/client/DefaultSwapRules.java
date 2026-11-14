package com.thevoos.megatools.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.network.ApplyDefaultRulesPayload;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Default custom rules: swap rules a Mega Tools tool gets by itself, to save the player setting up
 * commonly used tools by hand.
 *
 * File: config/megatools-default-rules.json (created with defaults on first launch).
 *
 * {
 *   "rules": {
 *     "evilcraft": [                                  // mod id: the group is only used if that mod is installed
 *       { "tool": "minecraft:flint_and_steel", "block": "evilcraft:hardened_blood" }
 *     ]
 *   }
 * }
 *
 * Each rule names the stored "tool" and exactly one of:
 *   "block": "modid:block"   only that block
 *   "tag":   "#modid:tag"    every block with that block tag
 *   "mod":   "modid"         every block from that mod
 *
 * A default rule is COPIED onto a tool the first time the named tool is in that tool's storage (see
 * offer). From then on it is an ordinary rule of that tool: the player can change or remove it in the
 * Custom Rules GUI like any filter they set themselves. Each tool remembers which default rules it
 * already received (ModDataComponents.APPLIED_DEFAULT_RULES), so a removed one does not come back; a
 * rule added to the file later is still copied.
 *
 * The file stays on the client on purpose: every player can tune their own defaults. It is re-read
 * every time you join a world or server.
 */
public final class DefaultSwapRules
{
    private static final String FILE_NAME = "megatools-default-rules.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static SwapRules rules = SwapRules.EMPTY;
    private static boolean loaded = false;
    /** File rule -> the filter it becomes on a tool (see asFilter). */
    private static final Map<SwapRules.Rule, SwapRules.Rule> FILTERS = new HashMap<>();

    private DefaultSwapRules() {}

    /** The default rules of the installed mods. */
    public static SwapRules get()
    {
        if (!loaded)
            reload();
        return rules;
    }

    /**
     * Copies the default rules this tool hasn't received yet onto it (the server does the saving, see
     * ApplyDefaultRulesPayload). Only rules whose tool is in the storage right now are copied, so a tool
     * doesn't collect rules for tools it never holds. Called by AutoSwapClient when the held tool, its
     * storage or its rules changed; cheap when there is nothing to do.
     */
    public static void offer(ItemStack held, ItemContainerContents storage)
    {
        SwapRules defaults = get();
        if (defaults.isEmpty() || storage == null
                || !(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
            return;

        List<SwapRules.Rule> applied = ToolData.getAppliedDefaults(held).rules();
        List<SwapRules.Rule> missing = null;
        for (SwapRules.Rule rule : defaults.rules())
        {
            if (applied.contains(rule) || !isStored(storage, rule.tool()))
                continue;
            if (missing == null)
                missing = new ArrayList<>();
            if (missing.size() < SwapRules.MAX_TOTAL && !missing.contains(rule))
                missing.add(rule);
        }
        if (missing == null || applied.size() + missing.size() > SwapRules.MAX_APPLIED)
            return;

        List<SwapRules.Rule> filters = new ArrayList<>(missing.size());
        for (SwapRules.Rule rule : missing)
            filters.add(asFilter(rule));
        PacketDistributor.sendToServer(new ApplyDefaultRulesPayload(new SwapRules(missing), new SwapRules(filters)));
    }

    private static boolean isStored(ItemContainerContents storage, ResourceLocation tool)
    {
        for (ItemStack stored : storage.nonEmptyItems())
            if (BuiltInRegistries.ITEM.getKey(stored.getItem()).equals(tool))
                return true;
        return false;
    }

    /**
     * A rule of the file as a filter of the Custom Rules GUI. A filter slot shows an item, and the file
     * names only a tag or a mod id for those rules, so a tag rule gets the first block of its tag and a
     * mod rule the first block (else item) of that mod. If there is none the rule is used as it is: it
     * still matches, its slot just shows a barrier.
     */
    private static SwapRules.Rule asFilter(SwapRules.Rule rule)
    {
        return FILTERS.computeIfAbsent(rule, key ->
        {
            ResourceLocation shown = switch (key.mode())
            {
                case ITEM -> null;
                case TAG -> BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, key.tag().orElseThrow()))
                        .flatMap(tag -> tag.stream().map(holder -> holder.value().asItem()).filter(item -> item != Items.AIR).findFirst())
                        .map(BuiltInRegistries.ITEM::getKey).orElse(null);
                case MOD ->
                {
                    String mod = key.target().getNamespace();
                    ResourceLocation any = null;
                    for (Item item : BuiltInRegistries.ITEM)
                    {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                        if (!id.getNamespace().equals(mod))
                            continue;
                        if (item instanceof BlockItem)
                        {
                            any = id;
                            break;
                        }
                        if (any == null)
                            any = id;
                    }
                    yield any;
                }
            };
            return shown == null ? key : new SwapRules.Rule(key.tool(), shown, key.mode(), key.tag());
        });
    }

    /** (Re)reads the file. Creates it with defaults if it doesn't exist. Never throws. */
    public static void reload()
    {
        loaded = true;
        FILTERS.clear();
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

            List<SwapRules.Rule> result = new ArrayList<>();
            int skippedMods = 0;
            if (root.has("rules") && root.get("rules").isJsonObject())
            {
                for (Map.Entry<String, JsonElement> group : root.getAsJsonObject("rules").entrySet())
                {
                    if (!ModList.get().isLoaded(group.getKey().trim().toLowerCase(Locale.ROOT)))
                    {
                        skippedMods++;
                        continue;
                    }
                    if (!group.getValue().isJsonArray())
                        continue;
                    for (JsonElement entry : group.getValue().getAsJsonArray())
                    {
                        SwapRules.Rule rule = entry.isJsonObject() ? readRule(entry.getAsJsonObject()) : null;
                        if (rule == null)
                            MegaTools.LOGGER.warn("[{}] ignoring invalid rule {}", FILE_NAME, entry);
                        else
                            result.add(rule);
                    }
                }
            }
            rules = new SwapRules(result);
            MegaTools.LOGGER.info("[{}] loaded {} default rule(s) ({} mod group(s) skipped, mod not loaded)",
                    FILE_NAME, result.size(), skippedMods);
        }
        catch (Exception e)
        {
            // A typo in the file must never crash the game: keep running without default rules.
            rules = SwapRules.EMPTY;
            MegaTools.LOGGER.error("[{}] could not be read, default rules disabled until it is fixed", FILE_NAME, e);
        }
    }

    /** One rule object, or null if it has no valid "tool" or not exactly one valid "block" / "tag" / "mod". */
    private static SwapRules.Rule readRule(JsonObject json)
    {
        ResourceLocation tool = id(json, "tool");
        int targets = (json.has("block") ? 1 : 0) + (json.has("tag") ? 1 : 0) + (json.has("mod") ? 1 : 0);
        if (tool == null || targets != 1)
            return null;

        if (json.has("block"))
        {
            ResourceLocation block = id(json, "block");
            return block == null ? null : new SwapRules.Rule(tool, block);
        }
        if (json.has("tag"))
        {
            // The target of a default tag rule is the tag itself: there is no block it was picked from.
            ResourceLocation tag = id(json, "tag");
            return tag == null ? null : new SwapRules.Rule(tool, tag, SwapRules.Mode.TAG, Optional.of(tag));
        }
        // Whole mod: only the namespace of the target matters (see SwapRules.Rule#matches).
        String mod = string(json, "mod");
        ResourceLocation target = mod == null ? null : ResourceLocation.tryBuild(mod, "any");
        return target == null ? null : new SwapRules.Rule(tool, target, SwapRules.Mode.MOD, Optional.empty());
    }

    private static String string(JsonObject json, String key)
    {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive())
            return null;
        String text = value.getAsString().trim().toLowerCase(Locale.ROOT);
        return text.isEmpty() ? null : text;
    }

    /** An id; a leading "#" (tags) is optional. */
    private static ResourceLocation id(JsonObject json, String key)
    {
        String text = string(json, key);
        if (text == null)
            return null;
        return ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);
    }

    private static void writeDefault(Path path) throws Exception
    {
        JsonObject root = new JsonObject();
        root.addProperty("_help",
                "Default custom rules of Mega Tools: which stored tool is pre-selected for which block. Rules are "
              + "grouped by mod id; a group is only used if that mod is installed. Each rule has a 'tool' (item id of "
              + "the stored tool) and exactly one of 'block' (a block id), 'tag' (a block tag, like #c:chests) or 'mod' "
              + "(a mod id: every block of that mod). A rule is added to a paxel or Mega Tool by itself, once, when the "
              + "tool is put in its tool storage. After that it is a normal filter in the Custom Rules menu and can be "
              + "changed or removed there; a removed one does not come back on that tool. This file is yours: edit it "
              + "to set the defaults you like. Use F3+H in game to see ids. Read when you join a world.");

        JsonObject exampleRule = new JsonObject();
        exampleRule.addProperty("tool", "minecraft:shears");
        exampleRule.addProperty("tag", "#minecraft:leaves");
        JsonArray exampleGroup = new JsonArray();
        exampleGroup.add(exampleRule);
        JsonObject example = new JsonObject();
        example.add("minecraft", exampleGroup);
        root.add("_example_rules", example);

        // Flint and Steel breaks EvilCraft's Hardened Blood into shards.
        JsonObject hardenedBlood = new JsonObject();
        hardenedBlood.addProperty("tool", "minecraft:flint_and_steel");
        hardenedBlood.addProperty("block", "evilcraft:hardened_blood");
        JsonArray evilcraft = new JsonArray();
        evilcraft.add(hardenedBlood);
        JsonObject groups = new JsonObject();
        groups.add("evilcraft", evilcraft);
        root.add("rules", groups);

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            GSON.toJson(root, writer);
        }
    }
}
