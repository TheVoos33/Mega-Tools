package com.thevoos.megatools.tool;

import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * The tool's light slot holds torches (#megatools:torches). While a tool with torches inside is held,
 * ToolLightManager lights the area at that torch's own brightness: torch 14, soul torch 10.
 */
public final class LightSources
{
    /** Light level for a tagged torch whose block doesn't report a light level itself. */
    public static final int DEFAULT_TORCH_LEVEL = 14;

    private LightSources() {}

    public static boolean isTorch(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(ModTags.TORCHES);
    }

    /** Light level (1-15) this torch gives, or 0 if it isn't an accepted torch. */
    public static int levelOf(ItemStack stack)
    {
        if (!isTorch(stack))
            return 0;
        if (stack.getItem() instanceof BlockItem blockItem)
        {
            int level = blockItem.getBlock().defaultBlockState().getLightEmission();
            if (level > 0)
                return level;
        }
        return DEFAULT_TORCH_LEVEL;
    }

    /** Brightest light from a Mega Tools tool in either hand (holding it "like a torch"), or 0. */
    public static int heldLevel(Player player)
    {
        return Math.max(toolLevel(player.getMainHandItem()), toolLevel(player.getOffhandItem()));
    }

    private static int toolLevel(ItemStack stack)
    {
        return stack.getItem() instanceof MultiToolItem ? levelOf(ToolData.getTorches(stack)) : 0;
    }
}
