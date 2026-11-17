package com.thevoos.megatools.mixin;

import com.thevoos.megatools.tool.MegaToolMining;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Mega Tools the last word on the mining speed: replaces the value Player#getDestroySpeed returns,
 * which is computed after vanilla's penalties and after every PlayerEvent.BreakSpeed listener of other
 * mods (see MegaToolMining for why an event listener wasn't enough). Priority above the default 1000,
 * so this also runs after other mods' injections at the same point.
 */
@Mixin(value = Player.class, priority = 1500)
public abstract class PlayerMixin
{
    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F", at = @At("RETURN"), cancellable = true)
    private void megatools$finalDigSpeed(BlockState state, @Nullable BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
        float speed = cir.getReturnValueF();
        float replaced = MegaToolMining.finalDigSpeed((Player) (Object) this, state, pos, speed);
        if (replaced != speed)
            cir.setReturnValue(replaced);
    }
}
