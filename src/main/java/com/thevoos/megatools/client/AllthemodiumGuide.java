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
 * The guide page about the Allthemodium Mega Tool upgrades (allthemodium_upgrades.md) lives in its own built-in
 * resource pack, resourcepacks/allthemodium_guide in the jar. The pack is only added when Allthemodium is
 * installed, always on and hidden from the resource pack screen. Without Allthemodium GuideME never sees the
 * page, so it doesn't show up in the guide (and its item_ids don't point at items that don't exist).
 * The pack also replaces the guide picture models_3d_2d.png with a version that shows the Allthemodium tiers
 * (made by Resources/Images/make_models_3d_2d.py --atm).
 */
public final class AllthemodiumGuide
{
    private static final String PACK_ID = "megatools_allthemodium_guide";

    private AllthemodiumGuide() {}

    /** Mod bus: add the hidden, always-on pack when Allthemodium is installed. */
    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.CLIENT_RESOURCES || !ModList.get().isLoaded("allthemodium"))
            return;
        Path root = ModList.get().getModFileById(MegaTools.MODID).getFile().findResource("resourcepacks", "allthemodium_guide");
        Pack.ResourcesSupplier resources = id -> new PathPackResources(id, root, true);
        Pack.Info info = Pack.readPackInfo(PACK_ID, resources);
        if (info == null)
        {
            MegaTools.LOGGER.warn("The built-in Allthemodium guide pack could not be read");
            return;
        }
        // Same info, but hidden from the resource pack screen; required = always on.
        Pack.Info hiddenInfo = new Pack.Info(info.description(), info.dataFormat(), info.resourceFormat(), info.requestedFeatures(), true);
        Pack pack = Pack.create(PACK_ID, Component.literal("Mega Tools: Allthemodium guide"), true, resources, hiddenInfo,
                PackType.CLIENT_RESOURCES, Pack.Position.TOP, false, PackSource.BUILT_IN);
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }
}
