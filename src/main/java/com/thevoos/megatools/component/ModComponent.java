package com.thevoos.megatools.component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
 * The stack's NBT is synced to clients by vanilla like any other item data.
 *
 * Decoded values are cached per NBT tag OBJECT. Reads happen every tick and every frame (held light,
 * auto-swap, mining speed, shader light), and decoding the storage means building up to 27 item stacks.
 * set() always stores a new tag object, and copying or syncing a stack creates new tag objects too, so a
 * cached value can't outlive the data it was read from. Like 1.21's data components, the values must never
 * be modified: copy them first.
 */
public final class ModComponent<T>
{
    /** Root key of all Mega Tools data in an item's NBT. */
    public static final String ROOT = "MegaTools";
    /** Cached for data that can't be read (Guava caches can't hold null). */
    private static final Object UNREADABLE = new Object();

    private final String name;
    private final Codec<T> codec;
    /** NBT tag (by identity: weakKeys() compares with ==) -> decoded value. Both sides use it: thread-safe. */
    private final Cache<Tag, Object> decoded = CacheBuilder.newBuilder().weakKeys().maximumSize(512).build();

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
    @SuppressWarnings("unchecked")
    public @Nullable T get(ItemStack stack)
    {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null)
            return null;
        Tag tag = root.get(name);
        if (tag == null)
            return null;
        Object value = decoded.getIfPresent(tag);
        if (value == null)
        {
            value = codec.parse(NbtOps.INSTANCE, tag)
                    .resultOrPartial(error -> MegaTools.LOGGER.warn("Ignoring unreadable Mega Tools data '{}' on {}: {}", name, stack, error))
                    .map(Object.class::cast)
                    .orElse(UNREADABLE);
            decoded.put(tag, value);
        }
        return value == UNREADABLE ? null : (T) value;
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
