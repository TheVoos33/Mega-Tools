package com.thevoos.megatools.items;

import com.thevoos.megatools.KeyNames;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * The enchanting table (and table mods using NeoForge's applyEnchantments hook) writes them straight
 * into the stored enchantments. Anything that writes normal enchantments instead (/enchant, other
 * mods) is moved over by inventoryTick; getStoredEnchantments counts both, so nothing is missed.
 *
 * ACCEPTED ENCHANTMENTS: only those that can go on one of the tools a paxel / Mega Tool replaces:
 * sword, shovel, axe, pickaxe, hoe, shears, brush or flint and steel (see allows). Armor, bow, trident,
 * fishing rod... enchantments are refused everywhere: enchanting table, anvil, /enchant, other mods.
 *
 * The normal ENCHANTMENTS component must stay present (empty): vanilla's ItemStack#isEnchantable
 * refuses items without it. It is therefore reset to empty, never removed.
 */
public class EnchantmentPlateItem extends Item
{
    /** Enchanting table "enchantability" (book 1, iron tools 14, gold 22). */
    private static final int ENCHANTMENT_VALUE = 14;

    public EnchantmentPlateItem(Item.Properties properties)
    {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** The tools whose enchantments a plate may hold (created on first use, after items are registered). */
    private static volatile List<ItemStack> referenceTools;
    /**
     * allows() answers per enchantment. They only change when tags reload (an enchantment's supported
     * items are usually a tag), so the cache is cleared on TagsUpdatedEvent. Concurrent: the client and the
     * integrated server both use it. Without it, every plate re-ran up to 8 tag checks per enchantment
     * every frame (glint) and every tick (inventoryTick).
     */
    private static final Map<Holder<Enchantment>, Boolean> ALLOWED = new ConcurrentHashMap<>();

    /**
     * True if this enchantment can go on a sword, shovel, axe, pickaxe, hoe, shears, brush or flint and
     * steel. Uses each enchantment's own "supported items", so modded enchantments for those tools work too.
     */
    public static boolean allows(Holder<Enchantment> enchantment)
    {
        Boolean cached = ALLOWED.get(enchantment);
        if (cached == null)
        {
            cached = computeAllows(enchantment);
            ALLOWED.put(enchantment, cached);
        }
        return cached;
    }

    private static boolean computeAllows(Holder<Enchantment> enchantment)
    {
        List<ItemStack> tools = referenceTools;
        if (tools == null)
            referenceTools = tools = List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_SHOVEL),
                    new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_HOE),
                    new ItemStack(Items.SHEARS), new ItemStack(Items.BRUSH), new ItemStack(Items.FLINT_AND_STEEL));
        for (ItemStack tool : tools)
            if (enchantment.value().isSupportedItem(tool))
                return true;
        return false;
    }

    /** Game bus (both sides): tags (and with them the supported items) changed, forget the cached answers. */
    public static void onTagsUpdated(TagsUpdatedEvent event)
    {
        ALLOWED.clear();
    }

    /** True if a plate accepts every one of these enchantments. */
    private static boolean allAllowed(ItemEnchantments enchantments)
    {
        for (Holder<Enchantment> enchantment : enchantments.keySet())
            if (!allows(enchantment))
                return false;
        return true;
    }

    /** These enchantments without the ones a plate doesn't accept (the same object if nothing is removed). */
    public static ItemEnchantments onlyAllowed(ItemEnchantments enchantments)
    {
        if (allAllowed(enchantments))
            return enchantments;
        ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet())
            if (allows(entry.getKey()))
                result.set(entry.getKey(), entry.getIntValue());
        return result.toImmutable();
    }

    /**
     * All enchantments the plate carries: stored ones plus any not yet moved over from /enchant or other
     * mods, without ones a plate doesn't accept (so a plate made before that rule can't load them on a tool).
     */
    public static ItemEnchantments getStoredEnchantments(ItemStack plate)
    {
        ItemEnchantments stored = plate.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments direct = plate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (direct.isEmpty())
            return onlyAllowed(stored); // the usual case: no new maps (called every frame for the glint)
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(stored);
        direct.entrySet().forEach(e -> merged.upgrade(e.getKey(), e.getIntValue()));
        return onlyAllowed(merged.toImmutable());
    }

    /** Which enchantments the plate accepts (enchanting table, anvil, /enchant and mods all ask this). */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment)
    {
        return allows(enchantment);
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

    /** Enchanting table (vanilla and mods using this NeoForge hook): store the enchantments like a book does. */
    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments)
    {
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(getStoredEnchantments(stack));
        for (EnchantmentInstance instance : enchantments)
            if (allows(instance.enchantment))
                stored.upgrade(instance.enchantment, instance.level);
        setStored(stack, stored.toImmutable());
        return stack;
    }

    /**
     * Server, while in an inventory: move normal enchantments (from /enchant or other mods) into the stored
     * ones and drop any a plate doesn't accept (plates made before that rule). Also repairs plates saved by
     * an older version, which removed the ENCHANTMENTS component and so could no longer be enchanted.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        if (level.isClientSide())
            return;
        ItemEnchantments direct = stack.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (direct == null || !direct.isEmpty() || !allAllowed(stored)) // runs every tick: no allocation unless needed
            setStored(stack, getStoredEnchantments(stack));
    }

    /** Puts these enchantments in the stored component and resets the normal one to its empty default. */
    public static void setStored(ItemStack plate, ItemEnchantments enchantments)
    {
        plate.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY); // equal to the default: no patch left
        if (enchantments.isEmpty())
            plate.remove(DataComponents.STORED_ENCHANTMENTS);
        else
            plate.set(DataComponents.STORED_ENCHANTMENTS, enchantments);
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
        tooltip.add(Component.translatable("tooltip.megatools.plate.usage", Component.keybind(KeyNames.OPEN_GUI)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
