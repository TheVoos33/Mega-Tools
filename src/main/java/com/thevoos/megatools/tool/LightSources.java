package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The tool's light slot holds torches (#megatools:torches). While a tool with torches inside is held
 * and its light is switched on, ToolLightManager lights the area at full brightness (15), whichever
 * torch it is, and shaders get the same level through MultiToolItem#getLightEmission.
 *
 * These checks run every server tick for every player and every frame for shaders, so they read the
 * torch component directly instead of copying the stored torch stack.
 */
public final class LightSources
{
    private LightSources() {}

    public static boolean isTorch(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(ModTags.TORCHES);
    }

    /** A Mega Tools tool with torches in its torch slot and its light switched on (GUI button under the slot). */
    public static boolean givesLight(ItemStack stack)
    {
        if (!(stack.getItem() instanceof MultiToolItem))
            return false;
        StoredTorches torches = ModDataComponents.TORCHES.get(stack);
        return torches != null && isTorch(torches.type()) && !ToolData.hasFlag(stack, ToolSettings.LIGHT_OFF);
    }

    /** Holding a lit Mega Tools tool in either hand ("like a torch"). */
    public static boolean isHoldingLight(Player player)
    {
        return givesLight(player.getMainHandItem()) || givesLight(player.getOffhandItem());
    }
}
