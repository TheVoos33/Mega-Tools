package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TorchBlock;

/**
 * The tool's light slot holds torches: any torch, vanilla or modded (see isTorch). While a tool with
 * torches inside is held and its light is switched on, ToolLightManager lights the area at full brightness (15), whichever
 * torch it is, and shaders get the same level through MultiToolItem#getLightEmission.
 *
 * These checks run every server tick for every player and every frame for shaders, so they read the
 * torch component directly instead of copying the stored torch stack.
 */
public final class LightSources
{
    private LightSources() {}

    /**
     * Any torch: the item tag #megatools:torches (for torches that fit none of the checks below), every item
     * that places a torch block (torch, soul torch, redstone torch and modded torches built on the vanilla
     * torch block), and every block item whose id is "torch" or ends with "_torch" (modded torches with
     * their own block class).
     */
    public static boolean isTorch(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        if (stack.is(ModTags.TORCHES))
            return true;
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return false;
        if (blockItem.getBlock() instanceof TorchBlock)
            return true;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("torch") || path.endsWith("_torch");
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
