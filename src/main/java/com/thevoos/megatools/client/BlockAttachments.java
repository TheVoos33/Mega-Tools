package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Things other mods attach to a block that isn't theirs, for tool auto-swap (client side): while the
 * player looks at one, the tools of the attachment's mod are pre-selected too, next to the
 * tools for the block itself (see AutoSwapClient).
 *
 *  - Entities sitting on the block, e.g. Logistics Network's Logistics Node, which covers any
 *    inventory. Which entity types count is the "attachments" list of megatools-tool-aliases.json
 *    (ToolAliases#attachmentEntities). The entity belongs to the block that contains its centre.
 *  - Transfer Labels: labels sit on a block's face, between that block and its neighbour, and are kept
 *    in the mod's own client-side storage, not in the world. Only looking at the label itself counts,
 *    not the blocks it connects. Read through reflection, so there is no dependency on the mod; if its classes ever change, this part
 *    switches itself off (logged once).
 *
 * Neither mod is needed to compile or run Mega Tools.
 */
public final class BlockAttachments
{
    private static final String TRANSFER_LABELS = "transfer_labels";

    // ---- Transfer Labels, looked up once (null = not installed, or the lookup failed) ----
    private static boolean labelsResolved;
    private static Method labelsGetStorage;   // static LabelStorage LabelClientStorage.getStorage(ClientLevel)
    private static Method labelsGetBlocksMap; // HashMap<BlockPos, LabelBlock> LabelStorage.getLabelBlocksMap()
    private static Method labelsGetShape;     // AABB LabelBlock.collectShapes(Direction), null without a label

    private BlockAttachments() {}

    /**
     * Mod ids of what the player is looking at besides the block itself: entities attached to the
     * targeted block, and a Transfer Label under the crosshair. Usually empty.
     */
    public static List<String> modsAt(ClientLevel level, Player player, BlockHitResult hit)
    {
        BlockPos pos = hit.getBlockPos();
        List<String> mods = List.of();

        Set<EntityType<?>> types = ToolAliases.attachmentEntities();
        if (!types.isEmpty())
        {
            // Attached entities can be a little larger than the block they cover: search a bit wider
            // and keep the ones whose centre is inside this block.
            for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(0.5D),
                    e -> types.contains(e.getType()) && BlockPos.containing(e.getBoundingBox().getCenter()).equals(pos)))
                mods = add(mods, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
        }

        if (isLookingAtTransferLabel(level, player, hit))
            mods = add(mods, TRANSFER_LABELS);
        return mods;
    }

    private static List<String> add(List<String> mods, String mod)
    {
        if (mods.contains(mod))
            return mods;
        if (mods.isEmpty())
            mods = new ArrayList<>(2);
        mods.add(mod);
        return mods;
    }

    /**
     * True if the crosshair is on a label, decided the way Transfer Labels itself decides it (its
     * RayTraceUtils): a label is a thin plate over one whole face of a block, and it is "looked at" when
     * the line of sight meets that plate before it meets the targeted block. Looking at another face of
     * the labelled block, or at the block next to it, does not count.
     */
    private static boolean isLookingAtTransferLabel(ClientLevel level, Player player, BlockHitResult hit)
    {
        if (!labelsResolved)
            resolveTransferLabels();
        if (labelsGetStorage == null)
            return false;
        try
        {
            Object storage = labelsGetStorage.invoke(null, level);
            if (storage == null || !(labelsGetBlocksMap.invoke(storage) instanceof Map<?, ?> blocks) || blocks.isEmpty())
                return false;

            // One ray for both distances. The hit result's own location comes from the last rendered
            // frame and this runs in the tick: mixing the two is off by more than a plate is thick
            // while the player moves, which made the result flicker.
            BlockPos pos = hit.getBlockPos();
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.getBlockReach() + 1.0D));
            BlockHitResult onBlock = level.getBlockState(pos).getShape(level, pos).clip(eye, end, pos);
            double blockDistance = onBlock == null ? Double.MAX_VALUE : eye.distanceTo(onBlock.getLocation());

            // A plate in front of the hit face belongs to the targeted block or to the block touching that face.
            if (labelHitBefore(blocks.get(pos), eye, end, blockDistance))
                return true;
            for (Direction direction : Direction.values())
                if (labelHitBefore(blocks.get(pos.relative(direction)), eye, end, blockDistance))
                    return true;
            return false;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            disableTransferLabels(e);
            return false;
        }
    }

    /** True if the line of sight meets one of this LabelBlock's label plates closer than {@code blockDistance}. */
    private static boolean labelHitBefore(Object labelBlock, Vec3 eye, Vec3 end, double blockDistance) throws ReflectiveOperationException
    {
        if (labelBlock == null)
            return false;
        for (Direction direction : Direction.values())
        {
            // AABB of the label on that face in world coordinates, null if the face has no label
            if (labelsGetShape.invoke(labelBlock, direction) instanceof AABB plate)
            {
                Optional<Vec3> point = plate.clip(eye, end);
                if (point.isPresent() && eye.distanceTo(point.get()) < blockDistance)
                    return true;
            }
        }
        return false;
    }

    private static void resolveTransferLabels()
    {
        labelsResolved = true;
        if (!ModList.get().isLoaded(TRANSFER_LABELS))
            return;
        try
        {
            Class<?> clientStorage = Class.forName("com.buuz135.transfer_labels.storage.client.LabelClientStorage");
            Class<?> storage = Class.forName("com.buuz135.transfer_labels.storage.LabelStorage");
            Class<?> labelBlock = Class.forName("com.buuz135.transfer_labels.storage.LabelBlock");
            labelsGetBlocksMap = storage.getMethod("getLabelBlocksMap");
            labelsGetShape = labelBlock.getMethod("collectShapes", Direction.class);
            labelsGetStorage = clientStorage.getMethod("getStorage", ClientLevel.class); // last: marks the lookup as complete
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            disableTransferLabels(e);
        }
    }

    private static void disableTransferLabels(Throwable cause)
    {
        labelsGetStorage = null;
        MegaTools.LOGGER.warn("Transfer Labels compatibility switched off: its label storage could not be read "
                + "(auto-swap will not pre-select tools for labels)", cause);
    }
}
