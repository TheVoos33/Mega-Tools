package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ItemEnchantments;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.SupportToolItem;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import com.thevoos.megatools.component.ItemContainerContents;
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
        return ModDataComponents.TOOL_SETTINGS.getOrDefault(tool, ToolSettings.DEFAULT);
    }

    public static void setSettings(ItemStack tool, ToolSettings settings)
    {
        ModDataComponents.TOOL_SETTINGS.set(tool, settings);
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
        ModDataComponents.TOOL_STORAGE.getOrDefault(tool, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setStorage(ItemStack tool, List<ItemStack> items)
    {
        ModDataComponents.TOOL_STORAGE.set(tool, ItemContainerContents.fromItems(items));
    }

    /** Number of stored tools (read-only, no copies). */
    public static int countStoredItems(ItemStack tool)
    {
        ItemContainerContents contents = ModDataComponents.TOOL_STORAGE.get(tool);
        if (contents == null)
            return 0;
        int count = 0;
        for (ItemStack ignored : contents.nonEmptyItems())
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
        return ModDataComponents.SWAP_RULES.getOrDefault(tool, SwapRules.EMPTY);
    }

    /** EMPTY removes the component, so a tool without rules is identical to a fresh one. */
    public static void setSwapRules(ItemStack tool, SwapRules rules)
    {
        if (rules.isEmpty())
            ModDataComponents.SWAP_RULES.remove(tool);
        else
            ModDataComponents.SWAP_RULES.set(tool, rules);
    }

    /** The default rules this tool already received (read-only); EMPTY if none. */
    public static SwapRules getAppliedDefaults(ItemStack tool)
    {
        return ModDataComponents.APPLIED_DEFAULT_RULES.getOrDefault(tool, SwapRules.EMPTY);
    }

    public static void setAppliedDefaults(ItemStack tool, SwapRules applied)
    {
        if (applied.isEmpty())
            ModDataComponents.APPLIED_DEFAULT_RULES.remove(tool);
        else
            ModDataComponents.APPLIED_DEFAULT_RULES.set(tool, applied);
    }

    // ---- Torch slot (light) -------------------------------------------------------------------

    /** The torches in the light slot as one stack (count up to 256), or EMPTY. */
    public static ItemStack getTorches(ItemStack tool)
    {
        StoredTorches torches = ModDataComponents.TORCHES.get(tool);
        return torches == null ? ItemStack.EMPTY : torches.asStack();
    }

    /** Stores the stack (type + count, count clamped to 256); EMPTY clears the slot. */
    public static void setTorches(ItemStack tool, ItemStack torches)
    {
        if (torches.isEmpty())
            ModDataComponents.TORCHES.remove(tool);
        else
            ModDataComponents.TORCHES.set(tool,
                    new StoredTorches(torches.copyWithCount(1), Math.min(torches.getCount(), StoredTorches.MAX)));
    }

    /** LEGACY single-item light slot (see ModDataComponents.TOOL_LIGHT), only used for migration. */
    public static ItemStack getLegacyLight(ItemStack tool)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        ModDataComponents.TOOL_LIGHT.getOrDefault(tool, ItemContainerContents.EMPTY).copyInto(items);
        return items.get(0);
    }


    // ---- Support Tool slot ---------------------------------------------------------------------

    /** The item in the Support Tool slot, or EMPTY. */
    public static ItemStack getSupportTool(ItemStack tool)
    {
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        ModDataComponents.SUPPORT_TOOL.getOrDefault(tool, ItemContainerContents.EMPTY).copyInto(items);
        return items.get(0);
    }

    /** EMPTY clears the slot. */
    public static void setSupportTool(ItemStack tool, ItemStack support)
    {
        if (support.isEmpty())
            ModDataComponents.SUPPORT_TOOL.remove(tool);
        else
            ModDataComponents.SUPPORT_TOOL.set(tool, ItemContainerContents.fromItems(List.of(support.copyWithCount(1))));
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
        ItemContainerContents slot = ModDataComponents.SUPPORT_TOOL.get(tool);
        if (slot == null)
            return false;
        Iterator<ItemStack> items = slot.nonEmptyItems().iterator();
        return items.hasNext() && items.next().getItem() instanceof SupportToolItem;
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
        ModDataComponents.ENCHANTMENT_PLATES.getOrDefault(tool, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setPlates(ItemStack tool, List<ItemStack> items)
    {
        ModDataComponents.ENCHANTMENT_PLATES.set(tool, ItemContainerContents.fromItems(items));
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
                ItemEnchantments.of(tool));
        if (!onTool.equals(EnchantmentPlateItem.getStoredEnchantments(plate)))
        {
            ItemStack updated = plate.copy();
            EnchantmentPlateItem.setStored(updated, onTool);
            plates.set(active, updated);
        }
        return plates;
    }

    private static boolean listMatches(List<ItemStack> a, List<ItemStack> b)
    {
        if (a.size() != b.size())
            return false;
        for (int i = 0; i < a.size(); i++)
            if (!ItemStack.matches(a.get(i), b.get(i)))
                return false;
        return true;
    }

    /** Writes the tool's current enchantments back onto its loaded plate (see getPlatesSynced). */
    public static void syncLoadedPlate(ItemStack tool)
    {
        NonNullList<ItemStack> synced = getPlatesSynced(tool);
        if (!listMatches(synced, getPlates(tool)))
            setPlates(tool, synced); // only when something actually changed
    }

    /**
     * Copies the enchantments of the currently selected plate onto the tool.
     * Because the tool's real enchantment list is replaced, every vanilla and modded
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

        ItemEnchantments.setOn(tool, enchantments);
    }
}
