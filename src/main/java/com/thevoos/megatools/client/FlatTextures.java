package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
/**
 * The built-in "Mega Tools Flat Textures" resource pack.
 *
 * The pack lives in the mod jar (resourcepacks/flat_textures) and shows up under Options > Resource Packs,
 * off by default. It replaces the 3D models of the paxels, the Mega Tools and the heads with flat 16x16
 * vanilla-style items.
 *
 * The Mega Tool animation is no longer tied to the pack (before v1.3 the pack switched the poke animation
 * off): it is the megaToolAnimation option of the config file, for the 3D model and the flat textures alike.
 */
public final class FlatTextures
{
    private static final String PACK_FOLDER = "resourcepacks/flat_textures";

    private FlatTextures() {}

    /** Mod bus: offer the pack in the resource pack screen (not enabled by default). */
    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        event.addPackFinders(MegaTools.id(PACK_FOLDER), PackType.CLIENT_RESOURCES,
                Component.translatable("pack.megatools.flat_textures"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }
}
