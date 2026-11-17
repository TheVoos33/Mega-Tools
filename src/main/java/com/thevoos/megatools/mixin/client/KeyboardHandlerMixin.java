package com.thevoos.megatools.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thevoos.megatools.client.ToolScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The swap key in the Mega Tools GUI (R on a stored tool) comes before recipe viewers. EMI reads key presses in
 * container screens before the screen and before Forge's screen key events, and shows the hovered item's
 * recipe on R; this runs at the very start of the key handling, so a quick swap wins and EMI never sees it.
 * Any other key, or R anywhere else, goes on as usual (see ToolScreen#tryQuickSwap).
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin
{
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void megatools$quickSwapFirst(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci)
    {
        if (action == GLFW.GLFW_PRESS
                && minecraft.screen instanceof ToolScreen toolScreen
                && window == minecraft.getWindow().getWindow()
                && !InputConstants.isKeyDown(window, GLFW.GLFW_KEY_F3) // F3 + key debug combinations
                && toolScreen.tryQuickSwap(key, scanCode))
            ci.cancel();
    }
}
