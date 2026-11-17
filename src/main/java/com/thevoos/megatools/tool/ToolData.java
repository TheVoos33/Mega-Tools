package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.SupportToolItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * Read/write helpers for the data stored on a Mega Tools tool stack.
 * All gameplay code goes through here so the component details live in one place.
 *
 * PERFORMANCE: the get...() methods return COPIES (safe to modify). Code that runs every tick or every
 * frame (light, mining speed, item abilities, GUI refresh, tooltips) uses the read-only checks below
 * instead (hasSupportTool, hasAnyItem, countStoredItems...), which look at the component without
 * copying any item stack.
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

    /** Number of stored tools (read-only, no copies). */
    public static int countStoredItems(ItemStack tool)
    {
        ItemContainerContents contents = tool.get(ModDataComponents.TOOL_STORAGE.get());
        if (contents == null)
            return 0;
        int count = 0;
        for (ItemStackTemplate ignored : contents.nonEmptyItems())
            count++;
        return count;
    }

    /** True if this item-container component holds at least one item (read-only, no copies). */
    public static boolean hasAnyItem(@Nullable ItemContainerContents contents)
    {
        return contents != null && contents.nonEmptyItems().iterator().hasNext();
    }

    // ---- Custom swap rules (Swap Rules GUI) ------------------------------------------------

    /** This tool's custom swap rules (read-only, no copies); EMPTY if it has none. */
    public static SwapRules getSwapRules(ItemStack tool)
    {
        return tool.getOrDefault(ModDataComponents.SWAP_RULES.get(), SwapRules.EMPTY);
    }

    /** EMPTY removes the component, so a tool without rules is identical to a fresh one. */
    public static void setSwapRules(ItemStack tool, SwapRules rules)
    {
        if (rules.isEmpty())
            tool.remove(ModDataComponents.SWAP_RULES.get());
        else
            tool.set(ModDataComponents.SWAP_RULES.get(), rules);
    }

    /** The default rules this tool already received (read-only); EMPTY if none. */
    public static SwapRules getAppliedDefaults(ItemStack tool)
    {
        return tool.getOrDefault(ModDataComponents.APPLIED_DEFAULT_RULES.get(), SwapRules.EMPTY);
    }

    public static void setAppliedDefaults(ItemStack tool, SwapRules applied)
    {
        if (applied.isEmpty())
            tool.remove(ModDataComponents.APPLIED_DEFAULT_RULES.get());
        else
            tool.set(ModDataComponents.APPLIED_DEFAULT_RULES.get(), applied);
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


    // ---- Support Tool slot ---------------------------------------------------------------------

    /** The item in the Support Tool slot, or EMPTY. */
    public static ItemStack getSupportTool(ItemStack tool)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        tool.getOrDefault(ModDataComponents.SUPPORT_TOOL.get(), ItemContainerContents.EMPTY).copyInto(items);
        return items.get(0);
    }

    /** EMPTY clears the slot. */
    public static void setSupportTool(ItemStack tool, ItemStack support)
    {
        if (support.isEmpty())
            tool.remove(ModDataComponents.SUPPORT_TOOL.get());
        else
            tool.set(ModDataComponents.SUPPORT_TOOL.get(), ItemContainerContents.fromItems(List.of(support.copyWithCount(1))));
    }

    /**
     * True if this tool has the shears / brush / flint and steel abilities: its tier has a Support
     * Tool slot and a Support Tool is in it. (Each ability also has its own GUI toggle.)
     */
    public static boolean hasSupportTool(ItemStack tool)
    {
        if (!(tool.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().supportTools())
            return false;
        // Hot path (mining speed, item abilities, GUI tick): look at the slot without copying the item.
        ItemContainerContents slot = tool.get(ModDataComponents.SUPPORT_TOOL.get());
        if (slot == null)
            return false;
        Iterator<ItemStackTemplate> items = slot.nonEmptyItems().iterator();
        return items.hasNext() && items.next().item().value() instanceof SupportToolItem;
    }

    /** True if the support ability behind {@code flag} (SHEAR, BRUSH or IGNITE) is usable right now. */
    public static boolean supportAbility(ItemStack tool, int flag)
    {
        return hasFlag(tool, flag) && hasSupportTool(tool); // cheap flag check first
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
     * The plates, with the LOADED one rewritten to hold exactly the tool's current enchantments.
     *
     * The tool's enchantments are a copy of the loaded plate's. Other mods can change them while the tool
     * sits in their machines (disenchanters such as Draconic Evolution's or SFM's move an enchantment onto
     * a book). If the plate kept its old enchantments, taking it out and putting it back would load the
     * removed enchantment again: a duplicate. So whenever a plate can leave the tool or be reloaded (GUI,
     * plate switch, crafting), the tool's current state is written back to the plate first. Only
     * enchantments a plate accepts are kept; the other plate slot isn't involved.
     */
    public static NonNullList<ItemStack> getPlatesSynced(ItemStack tool)
    {
        NonNullList<ItemStack> plates = getPlates(tool);
        if (!(tool.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasEnchantPlates())
            return plates;
        int active = Mth.clamp(getSettings(tool).activePlate(), PRIMARY, SECONDARY);
        ItemStack plate = plates.get(active);
        if (plate.isEmpty())
            return plates;

        ItemEnchantments onTool = EnchantmentPlateItem.onlyAllowed(
                tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        if (!onTool.equals(EnchantmentPlateItem.getStoredEnchantments(plate)))
        {
            ItemStack updated = plate.copy();
            EnchantmentPlateItem.setStored(updated, onTool);
            plates.set(active, updated);
        }
        return plates;
    }

    /** Writes the tool's current enchantments back onto its loaded plate (see getPlatesSynced). */
    public static void syncLoadedPlate(ItemStack tool)
    {
        NonNullList<ItemStack> synced = getPlatesSynced(tool);
        if (!ItemStack.listMatches(synced, getPlates(tool)))
            setPlates(tool, synced); // only when something actually changed
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

        // EMPTY (the default) rather than removing the component, so vanilla code always finds it.
        tool.set(DataComponents.ENCHANTMENTS, enchantments);
    }
}
