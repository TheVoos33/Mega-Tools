package com.thevoos.megatools;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional GuideME support. Mega Tools never needs GuideME: the guide itself is data driven
 * (assets/megatools/guideme_guides/guide.json + the pages in assets/megatools/guides/megatools/guide/),
 * which GuideME loads when it is installed and which is ignored otherwise. Java code only checks whether
 * GuideME is present and calls its public API by reflection, so there is no compile or runtime dependency.
 */
public final class GuideCompat
{
    public static final String MOD_ID = "guideme";
    public static final ResourceLocation GUIDE = MegaTools.id("guide");

    private static Boolean loaded;
    private static Method openGuide;
    private static Method pageAnchor;

    private GuideCompat() {}

    public static boolean isLoaded()
    {
        if (loaded == null)
            loaded = ModList.get().isLoaded(MOD_ID);
        return loaded;
    }

    /** Opens the Mega Tools guide at {@code page} (e.g. "tool_menu.md"), or its start page if null. */
    public static void open(Player player, String page)
    {
        if (!isLoaded())
            return;
        try
        {
            if (openGuide == null)
            {
                Class<?> guides = Class.forName("guideme.GuidesCommon");
                Class<?> anchor = Class.forName("guideme.PageAnchor");
                openGuide = guides.getMethod("openGuide", Player.class, ResourceLocation.class, anchor);
                pageAnchor = anchor.getMethod("page", ResourceLocation.class);
            }
            Object anchor = pageAnchor.invoke(null, MegaTools.id(page == null ? "index.md" : page));
            openGuide.invoke(null, player, GUIDE, anchor);
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            MegaTools.LOGGER.warn("Couldn't open the Mega Tools guide through GuideME", e);
        }
    }
}
