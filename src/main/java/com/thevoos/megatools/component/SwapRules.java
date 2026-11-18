package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom tool swap rules of ONE Mega Tools tool (set in the Swap Rules GUI, see SwapRulesScreen).
 *
 * Each rule says "when I look at this block, pre-select that stored tool":
 *   tool   - item id of the stored tool to pre-select
 *   target - item id of the block (or of a part, like an AE2 P2P tunnel) the player dragged into the filter slot
 *   mode   - ITEM: only that block, or that part of a block made of parts.  TAG: every block with one of its block tags (the rule's tag).
 *            MOD: every block from the target's mod
 *   blacklist - false: a whitelist filter, "pre-select the tool for these blocks".  true: a blacklist
 *            filter, "never pre-select the tool for these blocks" (also not through the automatic
 *            same-mod matching). When both lists match a block, the more exact filter wins
 *            (ITEM before TAG before MOD); at the same level the whitelist wins. See AutoSwapClient.
 *
 * Every stored tool has its own whitelist and its own blacklist, MAX_PER_TOOL filters each.
 *
 * Rules are stored on the tool stack, so every tool has its own. They are kept when the stored tool is
 * taken out (it is out of storage whenever it is swapped to); they simply do nothing until it is back.
 *
 * Items are saved as ids (not registry entries) on purpose: a rule for a mod that was removed must not
 * make the whole tool fail to load.
 *
 * Immutable: every change returns a new instance. The order of a tool's whitelist (or blacklist) rules
 * is the order of its filter slots in the GUI.
 */
public record SwapRules(List<Rule> rules)
{
    /** Filter slots per stored tool and list (whitelist / blacklist) in the GUI. */
    public static final int MAX_PER_TOOL = 18;
    /** Upper bound for one tool stack (keeps the item small on disk and on the network). */
    public static final int MAX_TOTAL = 1024;

    /** Upper bound for the list of default rules a tool already received (see APPLIED_CODEC). */
    public static final int MAX_APPLIED = 2048;

    public static final SwapRules EMPTY = new SwapRules(List.of());

    /** What a filter matches. The order is also the priority (most exact first) and the click order in the GUI. */
    public enum Mode
    {
        /** Only the target block. */
        ITEM("item"),
        /** Every block that has the rule's block tag. */
        TAG("tag"),
        /** Every block from the target's mod. */
        MOD("mod");

        private static final Mode[] VALUES = values();
        private final String name;

        Mode(String name)
        {
            this.name = name;
        }

        /** Unknown names and ids fall back to ITEM: a strange value must never make the tool fail to load. */
        static Mode byName(String name)
        {
            for (Mode mode : VALUES)
                if (mode.name.equals(name))
                    return mode;
            return ITEM;
        }

        static Mode byId(int id)
        {
            return id >= 0 && id < VALUES.length ? VALUES[id] : ITEM;
        }

        static final Codec<Mode> CODEC = Codec.STRING.xmap(Mode::byName, mode -> mode.name);
        static final StreamCodec<ByteBuf, Mode> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Mode::byId, Mode::ordinal);
    }

    /** Block tag keys of tag rules, so matching (on every rescan) doesn't create them again. */
    private static final Map<Identifier, TagKey<Block>> BLOCK_TAGS = new ConcurrentHashMap<>();

    private static TagKey<Block> blockTag(Identifier id)
    {
        if (BLOCK_TAGS.size() > 512)
            BLOCK_TAGS.clear(); // only the tags rules use are in it: this is never reached in normal play
        return BLOCK_TAGS.computeIfAbsent(id, tagId -> TagKey.create(Registries.BLOCK, tagId));
    }

    /**
     * @param tag       the block tag of a TAG rule (one of the target block's tags, picked in the GUI); empty otherwise
     * @param blacklist true for a blacklist filter (never pre-select the tool), false for a whitelist filter
     */
    public record Rule(Identifier tool, Identifier target, Mode mode, Optional<Identifier> tag, boolean blacklist)
    {
        /** "blacklist" is left out for whitelist rules, so rules saved before v1.2 load unchanged. */
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("tool").forGetter(Rule::tool),
                Identifier.CODEC.fieldOf("target").forGetter(Rule::target),
                Mode.CODEC.optionalFieldOf("mode", Mode.ITEM).forGetter(Rule::mode),
                Identifier.CODEC.optionalFieldOf("tag").forGetter(Rule::tag),
                Codec.BOOL.optionalFieldOf("blacklist", false).forGetter(Rule::blacklist)
        ).apply(instance, Rule::new));

        public static final StreamCodec<ByteBuf, Rule> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Rule::tool,
                Identifier.STREAM_CODEC, Rule::target,
                Mode.STREAM_CODEC, Rule::mode,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), Rule::tag,
                ByteBufCodecs.BOOL, Rule::blacklist,
                Rule::new);

        /** Keeps mode and tag consistent: only a TAG rule has a tag, and a TAG rule without one is an ITEM rule. */
        public Rule
        {
            if (mode == Mode.TAG && tag.isEmpty())
                mode = Mode.ITEM;
            else if (mode != Mode.TAG && tag.isPresent())
                tag = Optional.empty();
        }

        /** A whitelist filter. */
        public Rule(Identifier tool, Identifier target, Mode mode, Optional<Identifier> tag)
        {
            this(tool, target, mode, tag, false);
        }

        /** A new whitelist filter: only the target block. */
        public Rule(Identifier tool, Identifier target)
        {
            this(tool, target, Mode.ITEM, Optional.empty());
        }

        /** A new filter of either list: only the target block. */
        public Rule(Identifier tool, Identifier target, boolean blacklist)
        {
            this(tool, target, Mode.ITEM, Optional.empty(), blacklist);
        }

        /** The same filter with another mode (and tag), in the same list. */
        public Rule withMode(Mode mode, Optional<Identifier> tag)
        {
            return new Rule(tool, target, mode, tag, blacklist);
        }

        /**
         * @param block       the targeted block
         * @param blockId     its registry id
         * @param blockItemId registry id of its item (minecraft:air if it has none)
         * @param pickedId    registry id of the item "pick block" gives for the exact spot the player
         *                    looks at. For blocks made of parts (an AE2 cable bus, a conduit bundle...)
         *                    that is the part under the crosshair: a P2P tunnel, a cable, a bus.
         */
        public boolean matches(Block block, Identifier blockId, Identifier blockItemId, Identifier pickedId)
        {
            return switch (mode)
            {
                case ITEM -> target.equals(blockItemId) || target.equals(blockId) || target.equals(pickedId);
                case TAG -> block.builtInRegistryHolder().is(blockTag(tag.orElseThrow()));
                case MOD -> target.getNamespace().equals(blockId.getNamespace());
            };
        }
    }

    /** Disk format. Loaded lists are cleaned up, so hand-edited data can't exceed the limits. */
    public static final Codec<SwapRules> CODEC = Rule.CODEC.listOf().xmap(list -> new SwapRules(list).sanitized(), SwapRules::rules);

    /** Network format (synced to clients: the pre-selection runs there; and sent by the Swap Rules GUI). */
    public static final StreamCodec<ByteBuf, SwapRules> STREAM_CODEC =
            Rule.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TOTAL)).map(SwapRules::new, SwapRules::rules);

    /**
     * Disk and network format of the "applied default rules" list of a tool (ModDataComponents.APPLIED_DEFAULT_RULES):
     * the default rules of the config file that were already copied onto the tool. It is a plain list, kept
     * exactly as it is: the per-tool limits of real rules don't apply to it.
     */
    public static final Codec<SwapRules> APPLIED_CODEC = Rule.CODEC.listOf().xmap(SwapRules::new, SwapRules::rules);
    public static final StreamCodec<ByteBuf, SwapRules> APPLIED_STREAM_CODEC =
            Rule.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_APPLIED)).map(SwapRules::new, SwapRules::rules);

    public SwapRules
    {
        rules = List.copyOf(rules);
    }

    public boolean isEmpty()
    {
        return rules.isEmpty();
    }

    /** The whitelist ({@code blacklist} false) or blacklist rules of one stored tool, in filter slot order. */
    public List<Rule> forTool(Identifier tool, boolean blacklist)
    {
        List<Rule> result = new ArrayList<>();
        for (Rule rule : rules)
            if (rule.tool().equals(tool) && rule.blacklist() == blacklist)
                result.add(rule);
        return result;
    }

    /** Every tool that has at least one rule (either list), in first-rule order. */
    public Set<Identifier> tools()
    {
        Set<Identifier> result = new LinkedHashSet<>();
        for (Rule rule : rules)
            result.add(rule.tool());
        return result;
    }

    /**
     * Puts {@code target} into filter slot {@code index} of {@code tool}'s whitelist or blacklist: replaces the
     * filter that is there (a whole-mod filter stays one, anything else becomes "this block"), or adds a new
     * one if the slot is past the last filter.
     * Nothing changes if that list already has a filter for that target, or has no room left.
     */
    public SwapRules withTarget(Identifier tool, boolean blacklist, int index, Identifier target)
    {
        List<Rule> mine = forTool(tool, blacklist);
        for (Rule rule : mine)
            if (rule.target().equals(target))
                return this;

        List<Rule> changed = new ArrayList<>(rules);
        if (index >= 0 && index < mine.size())
            changed.set(globalIndex(tool, blacklist, index), mine.get(index).mode() == Mode.MOD
                    ? new Rule(tool, target, Mode.MOD, Optional.empty(), blacklist)
                    : new Rule(tool, target, blacklist)); // a tag belongs to the old block
        else if (mine.size() < MAX_PER_TOOL && rules.size() < MAX_TOTAL)
            changed.add(new Rule(tool, target, blacklist));
        else
            return this;
        return new SwapRules(changed);
    }

    /**
     * Switches filter {@code index} of {@code tool}'s whitelist or blacklist to its next mode: this block ->
     * one of its tags -> whole mod -> this block.
     *
     * @param blockTags the block tags of the filter's target (the caller looks them up); the tag mode
     *                  starts with the first one and is skipped if there are none
     */
    public SwapRules cycleMode(Identifier tool, boolean blacklist, int index, List<Identifier> blockTags)
    {
        int global = globalIndex(tool, blacklist, index);
        if (global < 0)
            return this;
        Rule old = rules.get(global);
        Rule next = switch (old.mode())
        {
            case ITEM -> blockTags.isEmpty()
                    ? old.withMode(Mode.MOD, Optional.empty())
                    : old.withMode(Mode.TAG, Optional.of(blockTags.getFirst()));
            case TAG -> old.withMode(Mode.MOD, Optional.empty());
            case MOD -> old.withMode(Mode.ITEM, Optional.empty());
        };
        List<Rule> changed = new ArrayList<>(rules);
        changed.set(global, next);
        return new SwapRules(changed);
    }

    /** Makes filter {@code index} of {@code tool}'s whitelist or blacklist match every block with this block tag. */
    public SwapRules withTag(Identifier tool, boolean blacklist, int index, Identifier tag)
    {
        int global = globalIndex(tool, blacklist, index);
        if (global < 0)
            return this;
        Rule old = rules.get(global);
        Rule next = old.withMode(Mode.TAG, Optional.of(tag));
        if (next.equals(old))
            return this;
        List<Rule> changed = new ArrayList<>(rules);
        changed.set(global, next);
        return new SwapRules(changed);
    }

    /** Removes filter {@code index} of {@code tool}'s whitelist or blacklist; the later filters of that list move up one slot. */
    public SwapRules without(Identifier tool, boolean blacklist, int index)
    {
        int global = globalIndex(tool, blacklist, index);
        if (global < 0)
            return this;
        List<Rule> changed = new ArrayList<>(rules);
        changed.remove(global);
        return new SwapRules(changed);
    }

    /**
     * These rules followed by {@code extra}, cleaned up: a rule of {@code extra} is left out if its tool's
     * list (whitelist or blacklist) already has a filter for that target or has no free filter slot.
     */
    public SwapRules withAll(SwapRules extra)
    {
        if (extra.isEmpty())
            return this;
        List<Rule> all = new ArrayList<>(rules);
        all.addAll(extra.rules());
        return new SwapRules(all).sanitized();
    }

    /**
     * A copy that respects every limit: no duplicate (tool, target) pairs within a list, at most MAX_PER_TOOL
     * per tool and list, and MAX_TOTAL overall. The server runs this on everything a client sends.
     */
    public SwapRules sanitized()
    {
        List<Rule> clean = new ArrayList<>();
        Set<Rule> seen = new HashSet<>();
        Map<ListKey, Integer> perList = new HashMap<>();
        for (Rule rule : rules)
        {
            if (clean.size() >= MAX_TOTAL)
                break;
            if (!seen.add(new Rule(rule.tool(), rule.target(), rule.blacklist())))
                continue;
            if (perList.merge(new ListKey(rule.tool(), rule.blacklist()), 1, Integer::sum) > MAX_PER_TOOL)
                continue;
            clean.add(rule);
        }
        return clean.size() == rules.size() ? this : new SwapRules(clean);
    }

    /** One filter list: the whitelist or the blacklist of one stored tool. */
    private record ListKey(Identifier tool, boolean blacklist) {}

    /** Position in {@link #rules} of the {@code index}-th whitelist or blacklist rule of {@code tool}, or -1. */
    private int globalIndex(Identifier tool, boolean blacklist, int index)
    {
        if (index < 0)
            return -1;
        int seen = 0;
        for (int i = 0; i < rules.size(); i++)
            if (rules.get(i).tool().equals(tool) && rules.get(i).blacklist() == blacklist && seen++ == index)
                return i;
        return -1;
    }
}
