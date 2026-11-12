package com.thevoos.megatools.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Enchantment Plate - an upgrade item that carries enchantments to a tool. Works like a book:
 *
 *  - Enchanting table (and enchanting mods): the plate is in the vanilla "enchantable" item tags
 *    for swords, axes, pickaxes, shovels, hoes and shears (data/minecraft/tags/item/enchantable/),
 *    so it is offered exactly the enchantments those tools can get.
 *  - Anvil: plate (left) + any enchanted book (right) adds the book's enchantments (PlateHelper).
 *  - Rename it in the anvil (e.g. "Silk Touch Plate"); Shift+R then shows "Silk Touch Plate loaded".
 *
 * Like an enchanted book, the enchantments are STORED on the plate (they don't act while you hold it).
 * The enchanting table writes normal enchantments, so inventoryTick moves those into the stored
 * enchantments; getStoredEnchantments also counts both, so nothing is missed in between.
 */
public class EnchantmentPlateItem extends Item
{
    /** Enchanting table "enchantability" (book 1, iron tools 14, gold 22). */
    private static final int ENCHANTMENT_VALUE = 14;

    public EnchantmentPlateItem(Item.Properties properties)
    {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** All enchantments the plate carries: stored ones plus any not yet moved over from the table. */
    public static ItemEnchantments getStoredEnchantments(ItemStack plate)
    {
        ItemEnchantments stored = plate.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments direct = plate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (direct.isEmpty())
            return stored;
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(stored);
        direct.entrySet().forEach(e -> merged.upgrade(e.getKey(), e.getIntValue()));
        return merged.toImmutable();
    }

    // ---- enchanting table ----

    /** Like a book: an unenchanted plate can go in the enchanting table. */
    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return stack.getCount() == 1 && getStoredEnchantments(stack).isEmpty();
    }

    @Override
    public int getEnchantmentValue()
    {
        return ENCHANTMENT_VALUE;
    }

    /** Move table enchantments into the stored (book-style) enchantments as soon as the plate is in an inventory. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        if (!level.isClientSide() && stack.has(DataComponents.ENCHANTMENTS))
        {
            ItemEnchantments all = getStoredEnchantments(stack);
            stack.remove(DataComponents.ENCHANTMENTS);
            stack.set(DataComponents.STORED_ENCHANTMENTS, all);
        }
    }

    // ---- looks ----

    /** Glint only once the plate actually holds enchantments. */
    @Override
    public boolean isFoil(ItemStack stack)
    {
        return !getStoredEnchantments(stack).isEmpty() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        if (getStoredEnchantments(stack).isEmpty())
            tooltip.add(Component.translatable("tooltip.megatools.plate.empty").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.megatools.plate.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
