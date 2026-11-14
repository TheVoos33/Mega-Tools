package com.thevoos.megatools.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.thevoos.megatools.MegaTools;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Saved format of the item slots on a tool (tool storage, plates, Support Tool): exactly vanilla's
 * ItemContainerContents format, but READ slot by slot.
 *
 * Vanilla reads the list as a whole, so one item that can't be read (its mod was removed from the
 * modpack) makes the whole component fail, and a failed component can take the tool and everything else
 * stored in it along. Tool storage is made for other mods' tools, so here such an item is skipped (and
 * logged) and the rest loads normally.
 */
public final class LenientContents
{
    private LenientContents() {}

    public static final Codec<ItemContainerContents> CODEC = Codec.of(ItemContainerContents.CODEC, new Decoder<>()
    {
        @Override
        public <T> DataResult<Pair<ItemContainerContents, T>> decode(DynamicOps<T> ops, T input)
        {
            return ops.getStream(input).map(slots ->
            {
                List<ItemStack> items = new ArrayList<>();
                slots.forEach(element ->
                {
                    Dynamic<T> slot = new Dynamic<>(ops, element);
                    int index = slot.get("slot").asInt(-1);
                    if (index < 0 || index > 255)
                        return;
                    Optional<ItemStack> stack = slot.get("item").result().flatMap(item -> ItemStack.CODEC.parse(item)
                            .resultOrPartial(error -> MegaTools.LOGGER.warn("Skipping an item stored in a Mega Tools tool that can't be loaded: {}", error)));
                    if (stack.isEmpty() || stack.get().isEmpty())
                        return;
                    while (items.size() <= index)
                        items.add(ItemStack.EMPTY);
                    items.set(index, stack.get());
                });
                return Pair.of(ItemContainerContents.fromItems(items), input);
            });
        }
    }, "MegaToolsContents");
}
