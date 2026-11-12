package com.thevoos.megatools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.MegaToolPoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Mega Tool "poke" animation, first person (applyForgeHandTransform) and third person (getArmPose,
 * see MegaToolArmPose). Both read the same MegaToolPoke state:
 *
 *  1. Left click held -> the tool turns ~90 degrees forward (TILT, 120 ms) and stays there.
 *  2. Each block that breaks -> one jab toward the block (THRUST, 220 ms). A jab never starts until the
 *     turn has finished, so it's always "turn, then prod".
 *  3. While left click stays held, the tool stays turned and just jabs again on every break.
 *  4. Release -> after the last jab it turns back upright.
 * Looking at a block without clicking does nothing, and vanilla's arm swing is ignored entirely.
 *
 * The first-person constants below control the look; adjust them if the tips don't line up with
 * the crosshair (they depend on the model's first-person display settings in mega_tool.json).
 */
public final class MegaToolClientExtensions
{
    /** How far the tool turns forward. */
    private static final float TILT_DEGREES = 90.0F;
    /** Turn the tips slightly toward the crosshair while tilted (degrees). */
    private static final float AIM_IN_DEGREES = 8.0F;
    /** Height of the pivot (the grip) relative to the item's centre: negative = lower. */
    private static final float GRIP_Y = -0.25F;
    /** How far the tool jabs forward (blocks). */
    private static final float THRUST_DISTANCE = 0.35F;
    /** Raise the tool while tilted so the tips sit near the crosshair (blocks). */
    private static final float LIFT = 0.12F;

    private MegaToolClientExtensions() {}

    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
    {
        event.registerItem(new IClientItemExtensions()
        {
            // ---------------- first person ----------------
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                   ItemStack itemInHand, float partialTick,
                                                   float equipProcess, float swingProcess)
            {
                // While brushing, keep vanilla's brush animation.
                if (player.isUsingItem() && player.getUseItem() == itemInHand)
                    return false;

                int side = arm == HumanoidArm.RIGHT ? 1 : -1;

                // Resting position: vanilla's ItemInHandRenderer#applyItemArmTransform with equip = 0.
                // equipProcess is ignored on purpose: vanilla lowers the item while the attack cooldown
                // recovers, which pushed the tool almost off screen when clicking rapidly.
                // swingProcess is ignored too, so held-click arm swings never move the tool.
                poseStack.translate(side * 0.56F, -0.52F, -0.72F);

                MegaToolPoke.State state = MegaToolPoke.updateLocal(localWantsTilt());
                float tilt = ease(state.tilt());
                float thrust = state.thrust();
                if (tilt <= 0.0F && thrust <= 0.0F)
                    return true; // idle: upright and still

                // Jab along the view direction (thrust is 0 until the turn has finished).
                poseStack.translate(0.0F, LIFT * tilt, -THRUST_DISTANCE * thrust);

                // Turn forward around the grip, tips slightly in toward the crosshair.
                poseStack.translate(0.0F, GRIP_Y, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * AIM_IN_DEGREES * tilt));
                poseStack.mulPose(Axis.XP.rotationDegrees(-TILT_DEGREES * tilt)); // negative X = top moves forward
                poseStack.translate(0.0F, -GRIP_Y, 0.0F);
                return true;
            }

            // ---------------- third person ----------------
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack itemStack)
            {
                if (entity.isUsingItem() && entity.getUseItem() == itemStack)
                    return null; // brushing: vanilla brush pose
                return MegaToolArmPose.poke();
            }
        }, ModItems.MEGA_TOOL.get());
    }

    /**
     * Local player is holding left click with a working Mega Tool (no screen open). This deliberately
     * does NOT depend on what the crosshair hits: while mining (especially upward) the targeted block
     * keeps disappearing and reappearing, which used to make the tilt flicker.
     */
    public static boolean localWantsTilt()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyAttack.isDown())
            return false;
        ItemStack held = mc.player.getMainHandItem();
        return MegaToolMining.isMegaTool(held) && !MultiToolItem.isBroken(held);
    }

    /** Smoothstep easing: slow start and end, no sudden jumps. */
    public static float ease(float x)
    {
        return x * x * (3.0F - 2.0F * x);
    }
}
