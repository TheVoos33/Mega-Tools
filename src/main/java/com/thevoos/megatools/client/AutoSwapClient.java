package com.thevoos.megatools.client;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.SwapOrigin;
import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tool auto-swap pre-selection (client side).
 *
 * WHEN it searches: only when the target changes (a different block type, another part of it, other
 * attachments on it, or looking away from / back at a block) or when the held tool, its storage or its swap rules change (hotbar
 * scroll, GUI edit, swap). Each tick only does a few cheap "did anything change?" comparisons; the
 * storage scan itself does not run every tick.
 *
 * WHAT it finds, in this order:
 *  1. Stored tools that one of the tool's custom swap rules (Swap Rules GUI, see SwapRules) names for
 *     the block: rules for exactly that block first, then tag rules, then whole-mod rules. These also work on vanilla blocks.
 *     "Exactly that block" includes the PART under the crosshair of blocks that are made of parts and
 *     have no item of their own (AE2 cable bus: P2P tunnels, cables, buses; conduit bundles...). The
 *     part is whatever item "pick block" gives for that spot (see pickedItem).
 *     The default rules of config/megatools-default-rules.json are among them: they are copied onto
 *     the tool when it is first held with the named tool in storage (see DefaultSwapRules#offer).
 *     Blacklist rules work the other way round: a stored tool whose blacklist matches the block is not
 *     pre-selected at all, neither by a whitelist rule nor by step 2. Only a whitelist rule that is MORE
 *     EXACT wins over it (or one at the same level: exact block / tag / whole mod). So "blacklist: whole
 *     mod AE2" + "whitelist: P2P tunnel" pre-selects the Memory Card for P2P tunnels and for no other AE2 block.
 *  2. Every other stored tool, in storage order, whose mod id (registry namespace) equals the block's
 *     mod id, or that config/megatools-tool-aliases.json maps to the block's mod id (see ToolAliases).
 *     Vanilla (minecraft:) blocks are skipped here: the paxel itself is the tool for those.
 *     If another mod attached something to the block (a Logistics Network node, a Transfer Label: see
 *     BlockAttachments), the stored tools of that mod come first, then the block's own. That also
 *     works on vanilla blocks.
 * The first is selected; Shift + mouse wheel cycles the selection (see
 * ClientEvents#onMouseScroll) and R swaps to the selected one.
 */
public final class AutoSwapClient
{
    // ---- current result ----
    private static final List<Integer> candidateSlots = new ArrayList<>();
    private static final List<ItemStack> candidates = new ArrayList<>();
    private static int selected = 0;
    /** Stored tools (item ids) a blacklist rule keeps from being pre-selected for the current block. */
    private static final Set<ResourceLocation> blocked = new HashSet<>();

    // ---- change detection: what the current result was computed for ----
    private static Block lastBlock = null;              // null = not looking at a block
    private static int lastHotbarSlot = -1;
    private static ItemStack lastHeld = ItemStack.EMPTY;
    private static ItemContainerContents lastStorage = null;
    private static SwapRules lastRules = SwapRules.EMPTY;
    private static List<String> lastAttached = List.of(); // mod ids attached to the targeted block
    private static Item lastPicked = Items.AIR;           // what "pick block" gives for the targeted spot

    // ---- what the crosshair was on when attachments and the picked part were last looked up ----
    /** Ticks after which they are looked up again anyway (a label or node added to the block in place). */
    private static final int LOOK_REFRESH = 10;
    private static BlockPos lookPos = null;               // null = nothing looked up
    private static Direction lookFace = null;
    private static int lookCell = -1;                     // 1/16 cell of the block the crosshair is in
    private static BlockState lookState = null;
    private static boolean lookWantsPick = false;
    private static int lookAge = 0;
    private static List<String> lookAttached = List.of();
    private static Item lookPicked = Items.AIR;

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
        int hotbarSlot = mc.player.getInventory().selected;
        ItemContainerContents storage = held.get(ModDataComponents.TOOL_STORAGE.get());
        SwapRules rules = ToolData.getSwapRules(held);
        BlockHitResult hit = targetedBlock(mc);
        Block block = hit == null ? null : mc.level.getBlockState(hit.getBlockPos()).getBlock();
        // Only tools with tool storage can swap, so only they pay for a closer look at the target.
        boolean canSwap = hit != null && held.getItem() instanceof MultiToolItem multiTool && multiTool.getTier().hasAutoSwap();
        if (canSwap)
            lookAtTarget(mc, hit, !rules.isEmpty()); // only rules look at the picked part
        else
            forgetLook();
        List<String> attached = canSwap ? lookAttached : List.of();
        Item picked = canSwap ? lookPicked : Items.AIR;

        boolean changed = block != lastBlock
                || hotbarSlot != lastHotbarSlot
                || held.getItem() != lastHeld.getItem()
                || !Objects.equals(storage, lastStorage)
                || !rules.equals(lastRules)
                || !attached.equals(lastAttached)
                || picked != lastPicked;
        if (!changed)
        {
            // Same contents, maybe a new object (the server re-sent the tool): keep the newest one, so
            // the next ticks hit equals()' identity check instead of comparing every stored item again.
            lastStorage = storage;
            lastRules = rules;
            return;
        }

        lastBlock = block;
        lastHotbarSlot = hotbarSlot;
        lastHeld = held;
        lastStorage = storage;
        lastRules = rules;
        lastAttached = attached;
        lastPicked = picked;
        // Not while the Custom Rules GUI is open: it sends complete rule lists of its own.
        if (!(mc.screen instanceof SwapRulesScreen))
            DefaultSwapRules.offer(held, storage);
        rescan(held, block, attached, picked);
    }

    /**
     * Looks up the attachments (reflection into other mods) and the picked part (another mod's code) of the
     * target, but only when the crosshair moved to another 1/16 of the block, another face or block state,
     * or LOOK_REFRESH ticks passed. Standing still and looking at a block then costs nothing.
     */
    private static void lookAtTarget(Minecraft mc, BlockHitResult hit, boolean wantsPick)
    {
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        int cell = cell(hit.getLocation(), pos);
        if (pos.equals(lookPos) && hit.getDirection() == lookFace && cell == lookCell && state == lookState
                && wantsPick == lookWantsPick && ++lookAge < LOOK_REFRESH)
            return;
        lookPos = pos.immutable();
        lookFace = hit.getDirection();
        lookCell = cell;
        lookState = state;
        lookWantsPick = wantsPick;
        lookAge = 0;
        lookAttached = BlockAttachments.modsAt(mc.level, mc.player, hit);
        lookPicked = wantsPick ? pickedItem(mc, hit) : Items.AIR;
    }

    private static void forgetLook()
    {
        lookPos = null;
        lookFace = null;
        lookState = null;
        lookAttached = List.of();
        lookPicked = Items.AIR;
    }

    /** Which 1/16 x 1/16 x 1/16 cell of the block at {@code pos} the point is in (0 to 4912). */
    private static int cell(Vec3 point, BlockPos pos)
    {
        int x = Mth.clamp(Mth.floor((point.x - pos.getX()) * 16), 0, 16);
        int y = Mth.clamp(Mth.floor((point.y - pos.getY()) * 16), 0, 16);
        int z = Mth.clamp(Mth.floor((point.z - pos.getZ()) * 16), 0, 16);
        return (x * 17 + y) * 17 + z;
    }

    /** The block the player looks at (with the exact spot), or null. */
    private static BlockHitResult targetedBlock(Minecraft mc)
    {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return null;
        return mc.level.getBlockState(hit.getBlockPos()).isAir() ? null : hit;
    }

    /**
     * The item the middle mouse button ("pick block") would give for the targeted spot. Blocks made of
     * parts answer with the part under the crosshair, which is how a rule can name a P2P tunnel or one
     * kind of cable although the block itself is the same for all of them. Air if the block gives nothing.
     */
    private static Item pickedItem(Minecraft mc, BlockHitResult hit)
    {
        try
        {
            BlockPos pos = hit.getBlockPos();
            return mc.level.getBlockState(pos).getCloneItemStack(hit, mc.level, pos, mc.player).getItem();
        }
        catch (RuntimeException e)
        {
            return Items.AIR; // another mod's block failed to answer: treat it as a block without parts
        }
    }

    private static void rescan(ItemStack held, Block block, List<String> attached, Item picked)
    {
        candidateSlots.clear();
        candidates.clear();
        blocked.clear();
        selected = 0;

        if (block == null || !(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
            return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        String blockMod = blockId.getNamespace();
        NonNullList<ItemStack> storage = ToolData.getStorage(held, multiTool.getTier().storageSlots());

        // ---- 1. This tool's custom rules (default rules are copied into them; blacklists fill `blocked`) ----
        addRuleMatches(ToolData.getSwapRules(held), storage, block, blockId, BuiltInRegistries.ITEM.getKey(picked));

        // ---- 2. Automatic matches (same mod / aliases), after the custom ones ----
        // First the mods of whatever is attached to the block (node, label...), then the block's own mod.
        for (String mod : attached)
            addModMatches(storage, mod);
        // Vanilla blocks only pre-select a stored tool through a custom rule or an attachment.
        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(blockMod))
            addModMatches(storage, blockMod);
    }

    /** Adds every stored tool, in storage order, that comes from this mod or is aliased to it (and isn't blacklisted here). */
    private static void addModMatches(NonNullList<ItemStack> storage, String mod)
    {
        for (int i = 0; i < storage.size(); i++)
        {
            ItemStack stored = storage.get(i);
            if (stored.isEmpty() || candidateSlots.contains(i))
                continue;
            ResourceLocation toolId = BuiltInRegistries.ITEM.getKey(stored.getItem());
            if (blocked.contains(toolId))
                continue;
            if (mod.equals(toolId.getNamespace()) || ToolAliases.matches(toolId, mod))
                addCandidate(i, stored);
        }
    }

    /**
     * Stored tools these rules name for the block: exact block first, then tag rules, then whole-mod rules.
     *
     * Blacklist rules go through the same levels. At each level the whitelist rules come first, then the
     * blacklist rules put their tool into {@code blocked}: a tool decided at an earlier (more exact) level
     * stays decided, and at the same level the whitelist wins.
     */
    private static void addRuleMatches(SwapRules rules, NonNullList<ItemStack> storage, Block block,
                                       ResourceLocation blockId, ResourceLocation pickedId)
    {
        if (rules.isEmpty())
            return;
        ResourceLocation blockItemId = BuiltInRegistries.ITEM.getKey(block.asItem());
        for (SwapRules.Mode mode : SwapRules.Mode.values())
        {
            for (SwapRules.Rule rule : rules.rules())
                if (!rule.blacklist() && rule.mode() == mode && !blocked.contains(rule.tool())
                        && rule.matches(block, blockId, blockItemId, pickedId))
                    addStoredTool(storage, rule.tool());
            for (SwapRules.Rule rule : rules.rules())
                if (rule.blacklist() && rule.mode() == mode && rule.matches(block, blockId, blockItemId, pickedId))
                    blocked.add(rule.tool()); // no effect on a tool that already is a candidate
        }
    }

    /** Adds every stored tool with this item id (normally one) that isn't a candidate yet. */
    private static void addStoredTool(NonNullList<ItemStack> storage, ResourceLocation toolId)
    {
        for (int i = 0; i < storage.size(); i++)
        {
            ItemStack stored = storage.get(i);
            if (!stored.isEmpty() && !candidateSlots.contains(i)
                    && BuiltInRegistries.ITEM.getKey(stored.getItem()).equals(toolId))
                addCandidate(i, stored);
        }
    }

    private static void addCandidate(int slot, ItemStack stored)
    {
        candidateSlots.add(slot);
        candidates.add(stored);
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
        blocked.clear();
        selected = 0;
        lastBlock = null;
        lastHotbarSlot = -1;
        lastHeld = ItemStack.EMPTY;
        lastStorage = null;
        lastRules = SwapRules.EMPTY;
        lastAttached = List.of();
        lastPicked = Items.AIR;
        forgetLook();
    }
}
