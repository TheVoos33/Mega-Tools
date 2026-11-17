package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The contents of a small container stored on a tool (storage, plates, Support Tool, legacy light slot).
 * Minecraft 1.20.1 has no item container component, so this is a small immutable stand-in for the one in
 * 1.21 (same name and methods), saved in the tool's NBT through ModComponent.
 */
public final class ItemContainerContents
{
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());

    /** One filled slot, as saved: {slot: 3, item: {id: ..., Count: ..., tag: ...}}. */
    private record Slot(int slot, ItemStack item)
    {
        static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 255).fieldOf("slot").forGetter(Slot::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Slot::item)
        ).apply(instance, Slot::new));
    }

    /** Strict format (LenientContents reads it more forgivingly). */
    public static final Codec<ItemContainerContents> CODEC = Slot.CODEC.listOf().xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);

    /** Every slot up to the last filled one; empty slots are ItemStack.EMPTY. Never modified. */
    private final List<ItemStack> items;

    private ItemContainerContents(List<ItemStack> items)
    {
        this.items = items;
    }

    /** Copies the stacks, so later changes to them don't change this. Trailing empty slots are dropped. */
    public static ItemContainerContents fromItems(List<ItemStack> stacks)
    {
        int size = stacks.size();
        while (size > 0 && stacks.get(size - 1).isEmpty())
            size--;
        if (size == 0)
            return EMPTY;
        List<ItemStack> copies = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            copies.add(stacks.get(i).copy());
        return new ItemContainerContents(Collections.unmodifiableList(copies));
    }

    static ItemContainerContents fromSlots(List<Slot> slots)
    {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : slots)
        {
            while (items.size() <= slot.slot())
                items.add(ItemStack.EMPTY);
            items.set(slot.slot(), slot.item());
        }
        return fromItems(items);
    }

    private List<Slot> asSlots()
    {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
            if (!items.get(i).isEmpty())
                slots.add(new Slot(i, items.get(i)));
        return slots;
    }

    /** Copies of the stored stacks into {@code destination} (as many slots as it has). */
    public void copyInto(NonNullList<ItemStack> destination)
    {
        for (int i = 0; i < destination.size(); i++)
            destination.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
    }

    /** The filled slots, NOT copied: read only. */
    public Iterable<ItemStack> nonEmptyItems()
    {
        return () -> items.stream().filter(stack -> !stack.isEmpty()).iterator();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ItemContainerContents other) || items.size() != other.items.size())
            return false;
        for (int i = 0; i < items.size(); i++)
            if (!ItemStack.matches(items.get(i), other.items.get(i)))
                return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 0;
        for (ItemStack stack : items)
            hash = 31 * hash + ModComponent.hashStack(stack);
        return hash;
    }
}
