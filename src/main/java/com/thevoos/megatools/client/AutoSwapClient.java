package com.thevoos.megatools.client;

import com.thevoos.megatools.Config;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.SwapOrigin;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tool auto-swap pre-selection (client side).
 *
 * WHEN it searches: only when the targeted block changes (a different block type, or looking
 * away from / back at a block) or when the held tool or its storage changes (hotbar scroll,
 * GUI edit, swap). Each tick only does a few cheap "did anything change?" comparisons; the
 * storage scan itself does not run every tick.
 *
 * WHAT it finds: every stored tool, in storage order, whose mod id (registry namespace) equals
 * the block's mod id, or that config/megatools-tool-aliases.json maps to the block's mod id
 * (see ToolAliases). The first is selected; Shift + mouse wheel cycles the selection (see
 * ClientEvents#onMouseScroll) and R swaps to the selected one.
 */
public final class AutoSwapClient
{
    // ---- current result ----
    private static final List<Integer> candidateSlots = new ArrayList<>();
    private static final List<ItemStack> candidates = new ArrayList<>();
    private static int selected = 0;

    // ---- change detection: what the current result was computed for ----
    private static Block lastBlock = null;              // null = not looking at a block
    private static int lastHotbarSlot = -1;
    private static ItemStack lastHeld = ItemStack.EMPTY;
    private static ItemContainerContents lastStorage = null;

    private AutoSwapClient() {}

    /** Called every client tick; only re-scans when the target or the held tool changed. */
    public static void update(Minecraft mc)
    {
        if (mc.player == null || mc.level == null)
        {
            clear();
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        Block block = targetedBlock(mc);
        int hotbarSlot = mc.player.getInventory().selected;
        ItemContainerContents storage = held.get(ModDataComponents.TOOL_STORAGE.get());

        boolean changed = block != lastBlock
                || hotbarSlot != lastHotbarSlot
                || held.getItem() != lastHeld.getItem()
                || !Objects.equals(storage, lastStorage);
        if (!changed)
            return;

        lastBlock = block;
        lastHotbarSlot = hotbarSlot;
        lastHeld = held;
        lastStorage = storage;
        rescan(held, block);
    }

    private static Block targetedBlock(Minecraft mc)
    {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return null;
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        return state.isAir() ? null : state.getBlock();
    }

    private static void rescan(ItemStack held, Block block)
    {
        candidateSlots.clear();
        candidates.clear();
        selected = 0;

        if (block == null || !(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
            return;

        String blockMod = BuiltInRegistries.BLOCK.getKey(block).getNamespace();
        // With autoSwapIgnoreVanilla, vanilla blocks only match tools explicitly aliased to "minecraft".
        boolean sameModMatches = !("minecraft".equals(blockMod) && Config.AUTO_SWAP_IGNORE_VANILLA.get());

        NonNullList<ItemStack> storage = ToolData.getStorage(held, multiTool.getTier().storageSlots());
        for (int i = 0; i < storage.size(); i++)
        {
            ItemStack stored = storage.get(i);
            if (stored.isEmpty())
                continue;
            ResourceLocation toolId = BuiltInRegistries.ITEM.getKey(stored.getItem());
            boolean match = (sameModMatches && blockMod.equals(toolId.getNamespace()))
                    || ToolAliases.matches(toolId, blockMod);
            if (match)
            {
                candidateSlots.add(i);
                candidates.add(stored);
            }
        }
    }

    /**
     * Shift + scroll: move the selection. Returns true if it moved (so the scroll should be
     * consumed instead of changing the hotbar slot).
     */
    public static boolean cycle(int direction)
    {
        if (candidates.size() < 2)
            return false;
        selected = Math.floorMod(selected + direction, candidates.size());
        return true;
    }

    /** Storage slot of the selected candidate, or -1 if nothing is pre-selected. */
    public static int getPreselectedSlot()
    {
        return candidateSlots.isEmpty() ? -1 : candidateSlots.get(selected);
    }

    public static List<ItemStack> getCandidates()
    {
        return Collections.unmodifiableList(candidates);
    }

    public static int getSelectedIndex()
    {
        return selected;
    }

    /** While a swapped-out tool is held: the Mega Tools tool that R swaps back to (else empty). */
    public static ItemStack getSwapBackIcon(Player player)
    {
        SwapOrigin origin = player.getMainHandItem().get(ModDataComponents.SWAP_ORIGIN.get());
        return origin != null ? origin.tool() : ItemStack.EMPTY;
    }

    public static void clear()
    {
        candidateSlots.clear();
        candidates.clear();
        selected = 0;
        lastBlock = null;
        lastHotbarSlot = -1;
        lastHeld = ItemStack.EMPTY;
        lastStorage = null;
    }
}
