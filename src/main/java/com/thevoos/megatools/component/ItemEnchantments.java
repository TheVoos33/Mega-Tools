package com.thevoos.megatools.component;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * An immutable set of enchantments with levels. Minecraft 1.20.1 keeps enchantments as NBT lists
 * ("Enchantments" for normal ones, "StoredEnchantments" for books and plates); this is a small stand-in
 * for the 1.21 class of the same name, with readers and writers for both lists.
 */
public final class ItemEnchantments
{
    public static final String NORMAL = "Enchantments";
    public static final String STORED = "StoredEnchantments";

    public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntLinkedOpenHashMap<>());

    private final Object2IntLinkedOpenHashMap<Enchantment> levels;

    private ItemEnchantments(Object2IntLinkedOpenHashMap<Enchantment> levels)
    {
        this.levels = levels;
    }

    // ---- reading and writing item stacks ----

    /** The normal (active) enchantments of a stack. */
    public static ItemEnchantments of(ItemStack stack)
    {
        return read(stack, NORMAL);
    }

    /** The stored enchantments of a book or plate. */
    public static ItemEnchantments stored(ItemStack stack)
    {
        return read(stack, STORED);
    }

    /** Replaces the normal enchantments of a stack (removes the list when empty). */
    public static void setOn(ItemStack stack, ItemEnchantments enchantments)
    {
        write(stack, NORMAL, enchantments);
    }

    /** Replaces the stored enchantments of a book or plate (removes the list when empty). */
    public static void setStoredOn(ItemStack stack, ItemEnchantments enchantments)
    {
        write(stack, STORED, enchantments);
    }

    private static ItemEnchantments read(ItemStack stack, String key)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key, Tag.TAG_LIST))
            return EMPTY;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty())
            return EMPTY;
        Mutable result = new Mutable(EMPTY);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(EnchantmentHelper.getEnchantmentId(entry));
            if (enchantment != null)
                result.upgrade(enchantment, EnchantmentHelper.getEnchantmentLevel(entry));
        }
        return result.toImmutable();
    }

    private static void write(ItemStack stack, String key, ItemEnchantments enchantments)
    {
        if (enchantments.isEmpty())
        {
            if (stack.getTag() != null)
                stack.removeTagKey(key);
            return;
        }
        ListTag list = new ListTag();
        for (Object2IntMap.Entry<Enchantment> entry : enchantments.entrySet())
            list.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(entry.getKey()), entry.getIntValue()));
        stack.getOrCreateTag().put(key, list);
    }

    // ---- contents ----

    public boolean isEmpty()
    {
        return levels.isEmpty();
    }

    public int size()
    {
        return levels.size();
    }

    public int getLevel(Enchantment enchantment)
    {
        return levels.getInt(enchantment);
    }

    public Set<Enchantment> keySet()
    {
        return java.util.Collections.unmodifiableSet(levels.keySet());
    }

    public Set<Object2IntMap.Entry<Enchantment>> entrySet()
    {
        return Object2IntMaps.unmodifiable(levels).object2IntEntrySet();
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o || o instanceof ItemEnchantments other && levels.equals(other.levels);
    }

    @Override
    public int hashCode()
    {
        return levels.hashCode();
    }

    /** A changeable copy; toImmutable() gives the result. */
    public static final class Mutable
    {
        private final Object2IntLinkedOpenHashMap<Enchantment> levels;

        public Mutable(ItemEnchantments start)
        {
            this.levels = new Object2IntLinkedOpenHashMap<>(start.levels);
        }

        /** Sets the level; 0 or less removes the enchantment. */
        public void set(Enchantment enchantment, int level)
        {
            if (level <= 0)
                levels.removeInt(enchantment);
            else
                levels.put(enchantment, Math.min(level, 255));
        }

        /** Keeps the higher of the current and the given level. */
        public void upgrade(Enchantment enchantment, int level)
        {
            if (level > levels.getInt(enchantment))
                set(enchantment, level);
        }

        public int getLevel(Enchantment enchantment)
        {
            return levels.getInt(enchantment);
        }

        public Set<Enchantment> keySet()
        {
            return levels.keySet();
        }

        public ItemEnchantments toImmutable()
        {
            return levels.isEmpty() ? EMPTY : new ItemEnchantments(new Object2IntLinkedOpenHashMap<>(levels));
        }
    }
}
