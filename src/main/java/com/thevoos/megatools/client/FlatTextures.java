package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;

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
    private static final String PACK_ID = "megatools_flat_textures";

    private FlatTextures() {}

    /** Mod bus: offer the pack in the resource pack screen (not enabled by default). */
    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.CLIENT_RESOURCES)
            return;
        Path root = ModList.get().getModFileById(MegaTools.MODID).getFile().findResource("resourcepacks", "flat_textures");
        Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.translatable("pack.megatools.flat_textures"), false,
                id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack == null)
        {
            MegaTools.LOGGER.warn("The built-in flat textures resource pack could not be read");
            return;
        }
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }
}
