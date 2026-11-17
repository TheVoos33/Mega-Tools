package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.thevoos.megatools.MegaTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * One piece of Mega Tools data on an item stack. Minecraft 1.20.1 has no data components, so each value is
 * written with its codec into the stack's NBT, under {MegaTools: {<name>: ...}}. The methods mirror the
 * ItemStack component methods of 1.21 (get, getOrDefault, has, set, remove), with the stack passed in.
 *
 * Values are decoded on every read; they are small (a few ints, at most 27 stacks), and the stack's NBT is
 * synced to clients by vanilla like any other item data.
 */
public final class ModComponent<T>
{
    /** Root key of all Mega Tools data in an item's NBT. */
    public static final String ROOT = "MegaTools";

    private final String name;
    private final Codec<T> codec;

    public ModComponent(String name, Codec<T> codec)
    {
        this.name = name;
        this.codec = codec;
    }

    public String name()
    {
        return name;
    }

    /** The value on this stack, or null if it has none (or it can't be read). */
    public @Nullable T get(ItemStack stack)
    {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null || !root.contains(name))
            return null;
        Tag tag = root.get(name);
        return codec.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> MegaTools.LOGGER.warn("Ignoring unreadable Mega Tools data '{}' on {}: {}", name, stack, error))
                .orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback)
    {
        T value = get(stack);
        return value != null ? value : fallback;
    }

    public boolean has(ItemStack stack)
    {
        CompoundTag root = stack.getTagElement(ROOT);
        return root != null && root.contains(name);
    }

    public void set(ItemStack stack, T value)
    {
        Objects.requireNonNull(value, name);
        Tag tag = codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(error -> MegaTools.LOGGER.error("Can't save Mega Tools data '{}': {}", name, error))
                .orElse(null);
        if (tag != null)
            stack.getOrCreateTagElement(ROOT).put(name, tag);
    }

    public void remove(ItemStack stack)
    {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null)
            return;
        root.remove(name);
        if (root.isEmpty())
            stack.removeTagKey(ROOT);
    }

    /** Hash of an item and its NBT (1.20.1 has no ItemStack#hashItemAndComponents). */
    public static int hashStack(ItemStack stack)
    {
        return stack.isEmpty() ? 0 : 31 * stack.getItem().hashCode() + Objects.hashCode(stack.getTag()) + stack.getCount();
    }
}
