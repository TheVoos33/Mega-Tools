package com.thevoos.megatools.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

/** The player an item on the ground is reserved for (1.20.1 has no getter). Used by TorchPickup. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor
{
    @Accessor("target")
    UUID megatools$getTarget();
}
