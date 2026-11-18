package com.thevoos.megatools.mixin.compat;

import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PickleTweaks' "Grid Repair" repairs any damaged tool in the crafting grid with its repair material (2 or 3
 * of them by default). It doesn't know Mega Tools' own repair recipes (each material repairs 1/8, see
 * recipe/ToolRepair), so with it installed a paxel was fully repaired with 2 ingots. This puts every Mega
 * Tools tool on its blacklist, as if the modpack had listed "megatools:*" in its config; repairing in an
 * anvil is not affected.
 *
 * Optional: in megatools.compat.mixins.json (not required), @Pseudo and require = 0, so without PickleTweaks,
 * or with a version that changed this method, nothing happens.
 */
@Pseudo
@Mixin(targets = "com.blakebr0.pickletweaks.feature.crafting.GridRepairHelper", remap = false)
public abstract class PickleTweaksGridRepairMixin
{
    @Inject(method = "isBlacklisted", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void megatools$ownRepairRecipes(Item item, CallbackInfoReturnable<Boolean> cir)
    {
        if (item instanceof MultiToolItem)
            cir.setReturnValue(true);
    }
}
