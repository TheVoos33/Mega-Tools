package com.thevoos.megatools.client;

import com.thevoos.megatools.tool.MegaToolPoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.IArmPoseTransformer;

/**
 * Third-person pose for the Mega Tool (F5 view and other players): a simple prod.
 *
 * The tool stays held exactly as mega_tool.json's third-person display sets it (no turning).
 * Each block break or hit plays one jab: the arm reaches forward and pulls back. While a poke is
 * active (left click held or a jab playing), vanilla's arm swing is switched off so the arm doesn't
 * chop up and down during the hold delay.
 *
 * Forge adds this arm pose to the vanilla ArmPose enum (ArmPose.create): not two-handed, with the
 * transformer below.
 */
public final class MegaToolArmPose
{
    /** How far the arm moves forward at the peak of a jab (model pixels). */
    private static final float THRUST_PIXELS = 3.0F;
    /** How far the arm swings forward at the peak of a jab (radians, ~30 degrees). */
    private static final float REACH_RADIANS = 0.5F;

    private static HumanoidModel.ArmPose pose;

    private MegaToolArmPose() {}

    /**
     * Adds the pose to the enum. Called once during client mod construction (MegaToolsClient#init):
     * HumanoidModel's arm pose switch builds its lookup table from the enum's size the first time a
     * player model is drawn, so a pose added after that is out of range and crashes the game.
     */
    public static void init()
    {
        if (pose == null)
            pose = HumanoidModel.ArmPose.create("MEGATOOLS_POKE", false, (IArmPoseTransformer) MegaToolArmPose::applyTransform);
    }

    public static HumanoidModel.ArmPose poke()
    {
        return pose;
    }

    private static void applyTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm)
    {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;

        MegaToolPoke.State state = entity == Minecraft.getInstance().player
                ? MegaToolPoke.updateLocal(MegaToolClientExtensions.localWantsTilt())
                : MegaToolPoke.updateRemote(entity.getId());
        float thrust = state == null ? 0.0F : state.thrust();
        boolean poking = state != null && (state.tilt() > 0.0F || thrust > 0.0F);

        // Vanilla's ArmPose.ITEM (walking swing halved, arm slightly forward), plus the jab.
        part.xRot = part.xRot * 0.5F - (float) Math.PI / 10.0F - REACH_RADIANS * thrust;
        part.yRot = 0.0F;
        part.z -= THRUST_PIXELS * thrust; // model -Z is forward

        if (poking)
            model.attackTime = 0.0F; // no vanilla swing on top of the prod
    }
}
