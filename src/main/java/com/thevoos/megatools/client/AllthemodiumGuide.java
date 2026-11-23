package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.resource.JarContentsPackResources;

import java.util.Optional;

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
    private static final String PACK_FOLDER = "resourcepacks/allthemodium_guide";

    private AllthemodiumGuide() {}

    /** Mod bus: add the hidden, always-on pack when Allthemodium is installed. */
    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.CLIENT_RESOURCES || !ModList.get().isLoaded("allthemodium"))
            return;
        Pack pack = Pack.readMetaAndCreate(
                new PackLocationInfo("mod/" + MegaTools.id(PACK_FOLDER), Component.literal("Mega Tools: Allthemodium guide"),
                        PackSource.BUILT_IN, Optional.empty()),
                new JarContentsPackResources.JarContentsResourcesSupplier(
                        ModList.get().getModFileById(MegaTools.MODID).getFile().getContents(), PACK_FOLDER),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, false)); // required = always on
        if (pack == null)
        {
            MegaTools.LOGGER.warn("The built-in Allthemodium guide pack could not be read");
            return;
        }
        Pack hidden = pack.hidden();
        event.addRepositorySource(consumer -> consumer.accept(hidden));
    }
}
