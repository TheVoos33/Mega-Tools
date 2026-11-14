package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.MultiToolItem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;


/** Everything to do with Enchantment Plates. */
public final class PlateHelper
{
    private PlateHelper() {}

    // =====================================================================================
    //  Filling plates: Enchantment Plate (left) + Enchanted Book (right) in an anvil
    // =====================================================================================

    public static void onAnvilUpdate(AnvilUpdateEvent event)
    {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!(left.getItem() instanceof EnchantmentPlateItem) || !right.is(Items.ENCHANTED_BOOK))
            return;

        ItemEnchantments base = EnchantmentPlateItem.getStoredEnchantments(left);
        // Only tool enchantments (sword, shovel, axe, pickaxe, hoe, shears, brush, flint and steel) go on a plate.
        ItemEnchantments added = EnchantmentPlateItem.onlyAllowed(
                right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        // No result: cancel, which empties the output. Vanilla's own anvil logic must never run for a plate:
        // it adds the book to the plate's NORMAL enchantments and checks compatibility against those
        // (always empty on a plate), so a Fortune book went onto a Silk Touch plate, or a book was used up
        // for nothing (same level already on the plate).
        if (added.isEmpty())
        {
            event.setCanceled(true); // e.g. a Protection book
            return;
        }

        ItemEnchantments merged = merge(base, added);
        if (merged.equals(base))
        {
            event.setCanceled(true); // nothing new: everything conflicted or is already at that level
            return;
        }

        ItemStack output = left.copy();
        EnchantmentPlateItem.setStored(output, merged); // normal enchantments are included in 'merged'

        // Our output replaces vanilla's, so apply the rename typed in the anvil ourselves.
        String name = event.getName();
        if (name != null)
        {
            if (StringUtil.isBlank(name))
                output.remove(DataComponents.CUSTOM_NAME);
            else if (!name.equals(left.getHoverName().getString()))
                output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }

        // XP cost: one level per accepted enchantment level on the book, capped below "Too Expensive!".
        int cost = 1;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : added.entrySet())
            cost += Math.max(1, entry.getIntValue());

        event.setOutput(output);
        event.setCost(Math.min(cost, 39));
        event.setMaterialCost(1);
    }

    /**
     * Anvil-style merge: same level -> level + 1 (up to max), otherwise the higher level wins.
     * Enchantments incompatible with ones already on the plate (e.g. Silk Touch vs Fortune) are skipped.
     */
    private static ItemEnchantments merge(ItemEnchantments base, ItemEnchantments added)
    {
        ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(base);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : added.entrySet())
        {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();

            boolean compatible = true;
            for (Holder<Enchantment> existing : result.keySet())
            {
                if (!existing.equals(enchantment) && !Enchantment.areCompatible(existing, enchantment))
                {
                    compatible = false;
                    break;
                }
            }
            if (!compatible)
                continue;

            int current = result.getLevel(enchantment);
            int newLevel = current == level
                    ? Math.min(level + 1, enchantment.value().getMaxLevel())
                    : Math.max(current, level);
            result.set(enchantment, newLevel);
        }
        return result.toImmutable();
    }

    // =====================================================================================
    //  Shift + R: switch between primary and secondary plate
    // =====================================================================================

    public static void switchPlate(ServerPlayer player)
    {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasEnchantPlates())
            return;

        ToolSettings settings = ToolData.getSettings(tool);
        int target = settings.activePlate() == ToolData.PRIMARY ? ToolData.SECONDARY : ToolData.PRIMARY;
        ToolData.syncLoadedPlate(tool); // the plate being unloaded keeps only what the tool still has
        NonNullList<ItemStack> plates = ToolData.getPlates(tool);
        ItemStack plate = plates.get(target);

        if (plate.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.megatools.plate_slot_empty",
                    Component.translatable(target == ToolData.PRIMARY ? "gui.megatools.primary" : "gui.megatools.secondary")), true);
            return;
        }

        ToolData.setSettings(tool, settings.withActivePlate(target));
        ToolData.applyActivePlate(tool);

        // Enchanting table sound + "Silk Touch Plate loaded" above the hotbar.
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.megatools.plate_loaded", plateDisplayName(plate)), true);
    }

    /**
     * The name shown in the "<name> loaded" message.
     * Renamed plate  -> its full name, e.g. "Silk Touch Plate loaded".
     * Unnamed plate  -> its enchantments, e.g. "Efficiency V, Unbreaking III loaded".
     * Returned as a Component so translation happens on the client (works on dedicated servers).
     */
    public static Component plateDisplayName(ItemStack plate)
    {
        if (plate.has(DataComponents.CUSTOM_NAME))
            return plate.getHoverName();

        MutableComponent list = Component.empty();
        boolean first = true;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentPlateItem.getStoredEnchantments(plate).entrySet())
        {
            if (!first) list.append(", ");
            list.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()));
            first = false;
        }
        return first ? plate.getHoverName() : list;
    }
}
