package com.thevoos.megatools.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thevoos.megatools.MegaTools;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.Reader;
import java.util.Optional;

/**
 * The built-in "Mega Tools Flat Textures" resource pack and the Mega Tool animation switch.
 *
 * The pack lives in the mod jar (resourcepacks/flat_textures) and shows up under Options > Resource Packs,
 * off by default. It replaces the 3D models of the paxels, the Mega Tool and the heads with flat 16x16
 * vanilla-style items.
 *
 * Animation switch: assets/megatools/mega_tool_animation.json, {"poke": true|false}. The mod's own copy
 * says true (the poke animation, tuned for the 3D model). The flat pack's copy says false, so the Mega Tool
 * uses vanilla's tool animations while it is enabled. Any resource pack can ship this file; the highest
 * pack that has it decides. Re-read on every resource reload.
 */
public final class FlatTextures
{
    private static final String PACK_FOLDER = "resourcepacks/flat_textures";
    private static final Identifier ANIMATION_FILE = MegaTools.id("mega_tool_animation.json");

    private static volatile boolean pokeAnimation = true;

    private FlatTextures() {}

    /** True: the Mega Tool's own poke animation. False: vanilla's tool animations. */
    public static boolean pokeAnimation()
    {
        return pokeAnimation;
    }

    /** Mod bus: offer the pack in the resource pack screen (not enabled by default). */
    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        event.addPackFinders(MegaTools.id(PACK_FOLDER), PackType.CLIENT_RESOURCES,
                Component.translatable("pack.megatools.flat_textures"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    /** Mod bus: read the animation switch whenever resource packs are (re)loaded. */
    public static void onRegisterReloadListeners(AddClientReloadListenersEvent event)
    {
        event.addListener(MegaTools.id("mega_tool_animation"), (ResourceManagerReloadListener) FlatTextures::reload);
    }

    private static void reload(ResourceManager manager)
    {
        pokeAnimation = readPoke(manager);
    }

    private static boolean readPoke(ResourceManager manager)
    {
        Optional<Resource> resource = manager.getResource(ANIMATION_FILE);
        if (resource.isEmpty())
            return true;
        try (Reader reader = resource.get().openAsReader())
        {
            JsonElement root = JsonParser.parseReader(reader);
            if (root instanceof JsonObject object && object.has("poke"))
                return object.get("poke").getAsBoolean();
        }
        catch (Exception e)
        {
            MegaTools.LOGGER.warn("Could not read {} from resource pack {}, using the poke animation",
                    ANIMATION_FILE, resource.get().sourcePackId(), e);
        }
        return true;
    }
}
