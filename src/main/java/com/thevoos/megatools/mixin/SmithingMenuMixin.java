package com.thevoos.megatools.mixin;

import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smithing upgrades of a Mega Tools tool (Netherite Paxel -> Mega Tool, the Allthemodium Mega Tool tiers) copy
 * everything from the old tool: stored tools, plates, torches, settings, custom rules, name. Durability is the
 * exception, like in the crafting carry-over recipes (ToolCarryOver): the upgraded tool is a fresh tool, so
 * the old tool's wear isn't moved onto a tool with a different maximum durability.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin
{
    @Inject(method = "createResult", at = @At("TAIL"))
    private void megatools$freshUpgradedTool(CallbackInfo ci)
    {
        ItemCombinerMenuAccessor menu = (ItemCombinerMenuAccessor) this;
        ItemStack result = menu.megatools$getResultSlots().getItem(0);
        ItemStack base = menu.megatools$getInputSlots().getItem(1);
        if (result.getItem() instanceof MultiToolItem && base.getItem() instanceof MultiToolItem && result.getItem() != base.getItem())
            result.removeTagKey("Damage");
    }
}
