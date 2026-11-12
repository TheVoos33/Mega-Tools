package com.thevoos.megatools.client;

import com.thevoos.megatools.tool.MegaToolPoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Third-person pose for the Mega Tool (F5 view and other players): a simple prod.
 *
 * The tool stays held exactly as mega_tool.json's third-person display sets it (no turning).
 * Each block break or hit plays one jab: the arm reaches forward and pulls back. While a poke is
 * active (left click held or a jab playing), vanilla's arm swing is switched off so the arm doesn't
 * chop up and down during the hold delay.
 *
 * NeoForge adds this arm pose to the vanilla ArmPose enum through META-INF/enumextensions.json
 * (listed in neoforge.mods.toml). POKE_PARAMS holds the constructor arguments: not two-handed,
 * and the transformer below.
 */
public final class MegaToolArmPose
{
    /** How far the arm moves forward at the peak of a jab (model pixels). */
    private static final float THRUST_PIXELS = 3.0F;
    /** How far the arm swings forward at the peak of a jab (radians, ~30 degrees). */
    private static final float REACH_RADIANS = 0.5F;

    public static final EnumProxy<HumanoidModel.ArmPose> POKE_PARAMS = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) MegaToolArmPose::applyTransform);

    private MegaToolArmPose() {}

    public static HumanoidModel.ArmPose poke()
    {
        return POKE_PARAMS.getValue();
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
