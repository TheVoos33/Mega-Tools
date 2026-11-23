package com.thevoos.megatools.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The input and result slots of the smithing table (and anvil) menu, for SmithingMenuMixin. */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor
{
    @Accessor("inputSlots")
    Container megatools$getInputSlots();

    @Accessor("resultSlots")
    ResultContainer megatools$getResultSlots();
}
