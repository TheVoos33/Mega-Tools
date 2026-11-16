package com.thevoos.megatools.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets MegaTools raise the upper limit of the attack damage attribute (see MegaTools#raiseAttackDamageLimit). */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor
{
    @Mutable
    @Accessor("maxValue")
    void megatools$setMaxValue(double maxValue);
}
