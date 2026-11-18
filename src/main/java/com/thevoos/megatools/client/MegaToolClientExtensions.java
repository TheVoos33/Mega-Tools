package com.thevoos.megatools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.MegaToolPoke;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
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
 * Looking at a block without clicking does nothing, and vanilla's arm swing is ignored while mining.
 * Right-click abilities (till, strip, path, shear, ignite, place torch...) play vanilla's normal tool
 * swing instead (MegaToolPoke#isUseSwing tells the two apart).
 *
 * Equip animation: vanilla's equipProcess also dips every time the attack cooldown restarts (each
 * click), which pushed the tool off screen while mining, so it isn't used directly. Instead:
 *  - Switching TO the Mega Tool: it slides up from below over EQUIP_MS (own timer, clicks don't touch it).
 *  - Switching AWAY: vanilla keeps drawing the Mega Tool while lowering it; only then is
 *    equipProcess followed, so it slides down instead of vanishing.
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

    // ---- equip slide ----
    /** How long the slide up takes when the tool is equipped. */
    private static final long EQUIP_MS = 300;
    /** How far below its resting place an un-equipped item sits (vanilla's value). */
    private static final float EQUIP_DROP = 0.6F;
    /** Only slide in if the tool was selected this recently (not when coming back from third person / F1). */
    private static final long EQUIP_WINDOW_MS = 1000;
    /** A gap in first-person drawing longer than this counts as the tool being shown anew. */
    private static final long SHOWN_GAP_MS = 250;
    private static final long NEVER = Long.MIN_VALUE / 4;

    // client tick: when the Mega Tool became the selected main-hand item
    private static long selectedAtMs = NEVER;
    private static int trackedSlot = -1;
    private static boolean trackedMega;
    // first-person drawing: which hotbar slot the drawn Mega Tool belongs to, and the slide timer
    private static int shownSlot = -1;
    private static long lastShownMs = NEVER;
    private static long equipStartMs = NEVER;

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
                // A resource pack switched the poke animation off (see FlatTextures): vanilla animations.
                if (!FlatTextures.pokeAnimation())
                    return false;
                // While brushing, keep vanilla's brush animation.
                if (player.isUsingItem() && player.getUseItem() == itemInHand)
                    return false;

                int side = arm == HumanoidArm.RIGHT ? 1 : -1;

                // Resting position: vanilla's ItemInHandRenderer#applyItemArmTransform, lowered only by our
                // own equip slide (see equipDown). Vanilla's equipProcess also dips on every click while
                // the attack cooldown recovers, which pushed the tool almost off screen when clicking rapidly.
                // swingProcess is ignored too, so held-click arm swings never move the tool.
                float down = equipDown(player, arm, itemInHand, equipProcess);
                poseStack.translate(side * 0.56F, -0.52F - EQUIP_DROP * down, -0.72F);

                // Right-click ability: vanilla's tool swing (ItemInHandRenderer, default item branch).
                if (swingProcess > 0.0F && arm == player.getMainArm() && MegaToolPoke.isUseSwing())
                    applyVanillaSwing(poseStack, side, swingProcess);

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
                if (!FlatTextures.pokeAnimation())
                    return null; // poke animation switched off by a resource pack: vanilla pose
                if (entity.isUsingItem() && entity.getUseItem() == itemStack)
                    return null; // brushing: vanilla brush pose
                return MegaToolArmPose.poke();
            }
        }, ModItems.MEGA_TOOL.get());
    }

    /** Vanilla's first-person tool swing: the arm offset plus ItemInHandRenderer#applyItemArmAttackTransform. */
    private static void applyVanillaSwing(PoseStack poseStack, int side, float swing)
    {
        float root = Mth.sqrt(swing);
        poseStack.translate(side * -0.4F * Mth.sin(root * Mth.PI),
                0.2F * Mth.sin(root * Mth.PI * 2.0F),
                -0.2F * Mth.sin(swing * Mth.PI));
        float f = Mth.sin(swing * swing * Mth.PI);
        float f1 = Mth.sin(root * Mth.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0F + f * -20.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * f1 * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }

    /** Client tick (ClientEvents): remember when the Mega Tool becomes the selected main-hand item. */
    public static void tickEquip(Minecraft mc)
    {
        if (mc.player == null)
        {
            trackedMega = false;
            trackedSlot = -1;
            return;
        }
        boolean mega = MegaToolMining.isMegaTool(mc.player.getMainHandItem());
        int slot = mc.player.getInventory().selected;
        if (mega && (!trackedMega || slot != trackedSlot))
            selectedAtMs = Util.getMillis();
        trackedMega = mega;
        trackedSlot = slot;
    }

    /**
     * How far the drawn tool is lowered: 0 = in place, 1 = fully below (vanilla's scale).
     * Main hand only; the off hand stays in place as before.
     */
    private static float equipDown(LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float equipProcess)
    {
        if (arm != player.getMainArm())
            return 0.0F;
        long now = Util.getMillis();
        ItemStack actual = player.getMainHandItem();
        int slot = player.getInventory().selected;

        // Switching away: vanilla still draws this Mega Tool while lowering it. Follow its drop.
        // (Same item in the same slot with a new stack object is just a data update, not a switch.)
        boolean leaving = itemInHand != actual && (!ItemStack.isSameItem(itemInHand, actual) || slot != shownSlot);
        if (leaving)
        {
            lastShownMs = now;
            return equipProcess;
        }

        // Newly drawn (other slot, or after not being drawn for a while) right after being selected: slide in.
        boolean newlyShown = slot != shownSlot || now - lastShownMs > SHOWN_GAP_MS;
        shownSlot = slot;
        lastShownMs = now;
        if (newlyShown && now - selectedAtMs <= EQUIP_WINDOW_MS)
            equipStartMs = now;

        float progress = (now - equipStartMs) / (float) EQUIP_MS;
        return progress >= 1.0F ? 0.0F : 1.0F - ease(Math.max(0.0F, progress));
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
