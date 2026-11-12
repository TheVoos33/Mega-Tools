package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/**
 * Read/write helpers for the data stored on a Mega Tools tool stack.
 * All gameplay code goes through here so the component details live in one place.
 */
public final class ToolData
{
    public static final int PLATE_SLOTS = 2;
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;

    private ToolData() {}

    // ---- Settings (toggles + active plate) -----------------------------------------------

    public static ToolSettings getSettings(ItemStack tool)
    {
        return tool.getOrDefault(ModDataComponents.TOOL_SETTINGS.get(), ToolSettings.DEFAULT);
    }

    public static void setSettings(ItemStack tool, ToolSettings settings)
    {
        tool.set(ModDataComponents.TOOL_SETTINGS.get(), settings);
    }

    public static boolean hasFlag(ItemStack tool, int flag)
    {
        return getSettings(tool).has(flag);
    }

    // ---- Wrench storage --------------------------------------------------------------------

    /** Returns a fresh, mutable copy of the storage with exactly {@code size} slots. */
    public static NonNullList<ItemStack> getStorage(ItemStack tool, int size)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        tool.getOrDefault(ModDataComponents.TOOL_STORAGE.get(), ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setStorage(ItemStack tool, List<ItemStack> items)
    {
        tool.set(ModDataComponents.TOOL_STORAGE.get(), ItemContainerContents.fromItems(items));
    }

    // ---- Torch slot (light) -------------------------------------------------------------------

    /** The torches in the light slot as one stack (count up to 256), or EMPTY. */
    public static ItemStack getTorches(ItemStack tool)
    {
        StoredTorches torches = tool.get(ModDataComponents.TORCHES.get());
        return torches == null ? ItemStack.EMPTY : torches.asStack();
    }

    /** Stores the stack (type + count, count clamped to 256); EMPTY clears the slot. */
    public static void setTorches(ItemStack tool, ItemStack torches)
    {
        if (torches.isEmpty())
            tool.remove(ModDataComponents.TORCHES.get());
        else
            tool.set(ModDataComponents.TORCHES.get(),
                    new StoredTorches(torches.copyWithCount(1), Math.min(torches.getCount(), StoredTorches.MAX)));
    }

    /** LEGACY single-item light slot (see ModDataComponents.TOOL_LIGHT), only used for migration. */
    public static ItemStack getLegacyLight(ItemStack tool)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        tool.getOrDefault(ModDataComponents.TOOL_LIGHT.get(), ItemContainerContents.EMPTY).copyInto(items);
        return items.get(0);
    }


    // ---- Enchantment plates ----------------------------------------------------------------

    public static NonNullList<ItemStack> getPlates(ItemStack tool)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(PLATE_SLOTS, ItemStack.EMPTY);
        tool.getOrDefault(ModDataComponents.ENCHANTMENT_PLATES.get(), ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setPlates(ItemStack tool, List<ItemStack> items)
    {
        tool.set(ModDataComponents.ENCHANTMENT_PLATES.get(), ItemContainerContents.fromItems(items));
    }

    /**
     * Copies the enchantments of the currently selected plate onto the tool.
     * Because the tool's real ENCHANTMENTS component is replaced, every vanilla and modded
     * enchantment (Silk Touch, Fortune, Efficiency, Mending, ...) works without any special code.
     */
    public static void applyActivePlate(ItemStack tool)
    {
        if (!(tool.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasEnchantPlates())
            return;

        int active = Mth.clamp(getSettings(tool).activePlate(), PRIMARY, SECONDARY);
        ItemStack plate = getPlates(tool).get(active);
        ItemEnchantments enchantments = plate.isEmpty()
                ? ItemEnchantments.EMPTY
                : EnchantmentPlateItem.getStoredEnchantments(plate);

        if (enchantments.isEmpty())
            tool.remove(DataComponents.ENCHANTMENTS);
        else
            tool.set(DataComponents.ENCHANTMENTS, enchantments);
    }
}
