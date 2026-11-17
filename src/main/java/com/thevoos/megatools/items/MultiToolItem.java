package com.thevoos.megatools.items;

import com.thevoos.megatools.client.MegaToolClientExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.thevoos.megatools.KeyNames;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.tool.LightSources;
import com.thevoos.megatools.tool.MegaToolPoke;
import com.thevoos.megatools.tool.ToolData;
import com.thevoos.megatools.tool.ToolLightManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * One class for all four tools (Stone Paxel, Iron Paxel, Diamond Paxel, Mega Tool).
 * Behaviour differences come from {@link ToolTier}.
 *
 * How the vanilla abilities are provided:
 *  - Mining: {@link #getDestroySpeed} and {@link #isCorrectToolForDrops} (pickaxe+axe+shovel+hoe tag,
 *    cobweb like a sword, leaves/wool like shears); harvest levels use the tier's vanilla Tier.
 *  - Right-click on blocks: we delegate to the vanilla AxeItem/ShovelItem/HoeItem/FlintAndSteelItem
 *    useOn() with OUR stack in the context. Those methods damage the stack in the context (ours),
 *    and Forge asks our {@link #canPerformAction} whether the action is allowed - which is where
 *    the GUI toggles are enforced.
 *  - Brushing: we start "using" the item and forward onUseTick() to the vanilla BrushItem.
 *  - Shearing mobs: delegated to the vanilla ShearsItem (Forge IForgeShearable: sheep, mooshroom,
 *    snow golem and modded mobs).
 *  - Shears, brush and flint and steel only work while a Support Tool is in the tool's Support Tool
 *    slot (ToolData#hasSupportTool), each with its own GUI toggle.
 */
public class MultiToolItem extends Item
{
    private final ToolTier tier;
    /** Main-hand attack damage and speed. */
    private final Multimap<Attribute, AttributeModifier> attributes;
    /** Mining speed of the pickaxe/axe/shovel/hoe blocks (read once, from the startup config). */
    private final float miningSpeed;

    public MultiToolItem(ToolTier tier, Item.Properties properties)
    {
        super(applyTier(tier, properties));
        this.tier = tier;
        this.attributes = createAttributes(tier);
        this.miningSpeed = tier.miningSpeed();
    }

    public ToolTier getTier()
    {
        return tier;
    }

    // =====================================================================================
    //  Never break: stop at 1 durability ("broken") so stored items are never lost
    // =====================================================================================

    /**
     * True when the tool is down to its last point of durability. A broken tool can't mine, attack or
     * use abilities until repaired (see the repair recipes), but its GUI, storage and plates still work.
     */
    public static boolean isBroken(ItemStack stack)
    {
        return stack.getItem() instanceof MultiToolItem
                && stack.isDamageableItem()
                && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /**
     * Forge hook called by ItemStack#hurtAndBreak before damage is applied. Trimming the amount so
     * the tool keeps at least 1 durability means vanilla's "durability reached 0 -> destroy the item"
     * step never happens, whatever caused the damage (mining, combat, abilities, other mods).
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken)
    {
        if (amount <= 0 || !stack.isDamageableItem())
            return amount; // repairs (negative amounts) and unbreakable tools are untouched
        int usableLeft = stack.getMaxDamage() - 1 - stack.getDamageValue();
        return Math.max(0, Math.min(amount, usableLeft));
    }

    /** Second safety net: even code that sets damage directly can't push the tool past 1 durability. */
    @Override
    public void setDamage(ItemStack stack, int damage)
    {
        int max = stack.getMaxDamage();
        super.setDamage(stack, max > 0 ? Math.min(damage, max - 1) : damage);
    }

    /** Action-bar hint shown when the player tries to use a broken tool (client side, no spam to the server). */
    public static void showBrokenMessage(Player player)
    {
        if (player.level().isClientSide())
            player.displayClientMessage(Component.translatable("message.megatools.tool_broken").withStyle(ChatFormatting.RED), true);
    }

    // =====================================================================================
    //  Item properties: attributes, durability, fire resistance
    // =====================================================================================

    private static Item.Properties applyTier(ToolTier tier, Item.Properties properties)
    {
        if (tier.durability() > 0)
            properties.durability(tier.durability());   // also forces stack size 1
        else
            properties.stacksTo(1);                       // Mega Tool: no durability = infinite use

        if (tier.isMega())
            properties.fireResistant().rarity(Rarity.EPIC); // survives fire and lava as a dropped item

        return properties;
    }

    /** Attack damage/speed. The player has 1 base damage, so the modifier is (total - 1). */
    private static Multimap<Attribute, AttributeModifier> createAttributes(ToolTier tier)
    {
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
                        tier.attackDamage() - 1.0D, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
                        tier.attackSpeed() - 4.0D, AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
    }

    // =====================================================================================
    //  Mining / combat
    // =====================================================================================

    /**
     * Only a real switch (other hotbar slot or other item) plays the lower-and-raise equip animation.
     * Forge's default re-equips whenever the stack object is replaced, which for these tools happens on
     * every data update (torch count, toggles, plates, durability sync).
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /**
     * Shader handheld light (Iris / Oculus 1.8+). Iris mixes its IrisItemLightProvider interface into every
     * Item and asks this method how much light a held item gives; shader packs with "handheld lighting"
     * then light the area smoothly around the player, without placing blocks. This method has exactly that
     * interface method's signature, so it is used when Iris is installed and simply unused otherwise (no
     * dependency on Iris). While a shader pack is active the client also tells the server to stop placing
     * light blocks for this player (see ShaderLightClient / ToolLightManager).
     */
    public int getLightEmission(Player player, ItemStack stack)
    {
        return LightSources.givesLight(stack) ? ToolLightManager.LIGHT_LEVEL : 0;
    }

    /**
     * Called whenever a swing starts (both sides). Client, local player, Mega Tool: lets the first-person
     * renderer tell a right-click ability swing from a mining swing (MegaToolPoke#onLocalSwing). The
     * vanilla swing itself always continues (false), so third person and other players see it as usual.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        if (tier.isMega() && entity.getMainHandItem() == stack && entity instanceof Player player && player.isLocalPlayer())
            MegaToolPoke.onLocalSwing();
        return false;
    }

    /**
     * The Mega Tool poke animation (first and third person). Forge calls this on the client only, from
     * Item's constructor, before this class has set tier, so the extensions check for a Mega Tool when
     * they run (other tiers fall back to vanilla).
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(MegaToolClientExtensions.create());
    }

    /**
     * Mining speed (first match wins): cobweb like a sword, then the shears speeds (leaves, wool, vines;
     * only while the shears ability is active), then pickaxe + axe + shovel + hoe blocks.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state)
    {
        if (state.is(Blocks.COBWEB))
            return 15.0F;
        if (tier.supportTools() && ToolData.supportAbility(stack, ToolSettings.SHEAR))
        {
            if (state.is(BlockTags.LEAVES))
                return 15.0F;
            if (state.is(BlockTags.WOOL))
                return 5.0F;
            if (state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN))
                return 2.0F;
        }
        return state.is(ModTags.MINEABLE_WITH_PAXEL) ? miningSpeed : 1.0F;
    }

    /**
     * Drops: cobwebs, and the pickaxe/axe/shovel/hoe blocks this tier is strong enough for (obsidian
     * mines with a stone tool but drops nothing). The Mega Tool counts as the correct tool for every block.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
    {
        if (isBroken(stack))
            return false; // broken: no tool drops, like an empty hand
        if (tier.isMega() || state.is(Blocks.COBWEB))
            return true;
        return state.is(ModTags.MINEABLE_WITH_PAXEL) && TierSortingRegistry.isCorrectTierForDrops(tier.harvestTier(), state);
    }

    /** 1 durability per block that isn't instantly broken (the Mega Tool takes none). */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner)
    {
        if (!level.isClientSide() && !tier.isMega() && state.getDestroySpeed(level, pos) != 0.0F)
            stack.hurtAndBreak(1, miner, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    /** Sword behaviour: lose 1 durability per hit (no-op for the unbreakable Mega Tool). */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    /**
     * Forge "tool actions": which vanilla tool actions this item can perform.
     * Used by block interactions (stripping, pathing, tilling), shear loot checks, sweep attacks, etc.
     * The right-click abilities honour the GUI toggles stored on the stack.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction ability)
    {
        if (isBroken(stack))
            return false; // broken: no stripping, shearing, sweeping...
        ToolSettings settings = ToolData.getSettings(stack);

        if (ability == ToolActions.AXE_STRIP || ability == ToolActions.AXE_SCRAPE || ability == ToolActions.AXE_WAX_OFF)
            return settings.has(ToolSettings.STRIP);
        if (ability == ToolActions.SHOVEL_FLATTEN)
            return settings.has(ToolSettings.PATH);
        if (ability == ToolActions.HOE_TILL)
            return settings.has(ToolSettings.TILL);
        // Support Tool abilities: other mods' blocks and loot tables ask for these too.
        if (ToolActions.DEFAULT_SHEARS_ACTIONS.contains(ability))
            return ToolData.supportAbility(stack, ToolSettings.SHEAR);

        return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                || ToolActions.DEFAULT_AXE_ACTIONS.contains(ability)
                || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(ability)
                || ToolActions.DEFAULT_HOE_ACTIONS.contains(ability)
                || ToolActions.DEFAULT_SWORD_ACTIONS.contains(ability);
    }

    // =====================================================================================
    //  Right click on a block: strip -> path/till -> brush -> trim -> TNT -> torch -> ignite
    // =====================================================================================

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        ItemStack stack = context.getItemInHand();
        if (isBroken(stack))
        {
            if (context.getPlayer() != null)
                showBrokenMessage(context.getPlayer());
            return InteractionResult.PASS;
        }
        ToolSettings settings = ToolData.getSettings(stack);
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // 1. Axe: strip logs, scrape/un-wax copper.
        if (settings.has(ToolSettings.STRIP))
        {
            InteractionResult result = Items.IRON_AXE.useOn(context);
            if (result.consumesAction()) return result;
        }

        // 2. Shovel path vs hoe till: both work on grass/dirt, so sneaking picks the hoe first.
        boolean sneaking = player != null && player.isSecondaryUseActive();
        Item first = sneaking ? Items.IRON_HOE : Items.IRON_SHOVEL;
        Item second = sneaking ? Items.IRON_SHOVEL : Items.IRON_HOE;
        InteractionResult result = tryDigAction(first, settings, context);
        if (result.consumesAction()) return result;
        result = tryDigAction(second, settings, context);
        if (result.consumesAction()) return result;

        boolean support = ToolData.hasSupportTool(stack);

        // 3. Brush: start "using" the item; brushing happens in onUseTick.
        if (support && settings.has(ToolSettings.BRUSH) && player != null
                && level.getBlockEntity(pos) instanceof BrushableBlockEntity)
        {
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        // 4. Shears: trim growing plants (cave vines, kelp, weeping/twisting vines) so they stop growing.
        if (support && settings.has(ToolSettings.SHEAR))
        {
            InteractionResult trim = Items.SHEARS.useOn(context);
            if (trim.consumesAction()) return trim;
        }

        // 5. Flint and steel on TNT: vanilla TNT only accepts the real flint and steel item.
        if (support && settings.has(ToolSettings.IGNITE))
        {
            InteractionResult tnt = tryIgniteTnt(context, stack);
            if (tnt.consumesAction()) return tnt;
        }

        // 6. Place Torch: put one torch from the torch slot on the clicked face.
        if (settings.has(ToolSettings.PLACE_TORCH))
        {
            InteractionResult torch = tryPlaceTorch(context, stack);
            if (torch.consumesAction()) return torch;
        }

        // 7. Flint and steel: light campfires/candles or place fire.
        if (support && settings.has(ToolSettings.IGNITE))
        {
            InteractionResult ignite = Items.FLINT_AND_STEEL.useOn(context);
            if (ignite.consumesAction()) return ignite;
        }
        return InteractionResult.PASS;
    }

    /** Lights TNT exactly like flint and steel does (TntBlock checks for the flint and steel item itself). */
    private static InteractionResult tryIgniteTnt(UseOnContext context, ItemStack tool)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TntBlock))
            return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (!level.isClientSide())
        {
            state.onCaughtFire(level, pos, context.getClickedFace(), player);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            if (player != null)
            {
                tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
                player.awardStat(Stats.ITEM_USED.get(Items.FLINT_AND_STEEL));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Places one torch from the torch slot exactly like right-clicking with the torch itself (standing or
     * wall torch, sound, block updates). A one-item copy is placed; the tool's count only goes down on
     * the server, and not in creative.
     */
    private static InteractionResult tryPlaceTorch(UseOnContext context, ItemStack tool)
    {
        ItemStack torches = ToolData.getTorches(tool);
        if (torches.isEmpty() || !(torches.getItem() instanceof BlockItem blockItem))
            return InteractionResult.PASS;

        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
        BlockPlaceContext place = new BlockPlaceContext(context.getLevel(), context.getPlayer(),
                context.getHand(), torches.copyWithCount(1), hit);
        InteractionResult result = blockItem.place(place);

        Player player = context.getPlayer();
        if (result.consumesAction() && !context.getLevel().isClientSide()
                && (player == null || !player.getAbilities().instabuild))
            ToolData.setTorches(tool, torches.copyWithCount(torches.getCount() - 1)); // 0 left -> slot empty
        return result;
    }

    /**
     * One-time move from the old single-item light slot (TOOL_LIGHT) to the torch slot. A torch moves
     * over; anything else (lantern, glowstone...) is given back to the player.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        if (level.isClientSide() || !ModDataComponents.TOOL_LIGHT.has(stack))
            return;
        ItemStack legacy = ToolData.getLegacyLight(stack);
        ModDataComponents.TOOL_LIGHT.remove(stack);
        if (legacy.isEmpty())
            return;

        ItemStack current = ToolData.getTorches(stack);
        boolean fits = LightSources.isTorch(legacy)
                && (current.isEmpty() || ItemStack.isSameItemSameTags(current, legacy))
                && current.getCount() + legacy.getCount() <= StoredTorches.MAX;
        if (fits)
            ToolData.setTorches(stack, legacy.copyWithCount(current.getCount() + legacy.getCount()));
        else if (entity instanceof Player player && !player.getInventory().add(legacy))
            player.drop(legacy, false);
    }

    private static InteractionResult tryDigAction(Item vanillaTool, ToolSettings settings, UseOnContext context)
    {
        if (vanillaTool == Items.IRON_SHOVEL && !settings.has(ToolSettings.PATH)) return InteractionResult.PASS;
        if (vanillaTool == Items.IRON_HOE && !settings.has(ToolSettings.TILL)) return InteractionResult.PASS;
        return vanillaTool.useOn(context);
    }

    // =====================================================================================
    //  Brush support (held right click)
    // =====================================================================================

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return ToolData.hasSupportTool(stack) ? 200 : 0; // same duration as the vanilla brush
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return ToolData.hasSupportTool(stack) ? UseAnim.BRUSH : UseAnim.NONE;
    }

    /** Forward each use tick to the vanilla brush logic (raycast, particles, sound, BrushableBlockEntity#brush). */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration)
    {
        if (ToolData.supportAbility(stack, ToolSettings.BRUSH) && !isBroken(stack))
            Items.BRUSH.onUseTick(level, entity, stack, remainingUseDuration);
        else
            entity.releaseUsingItem();
    }

    // =====================================================================================
    //  Shears on entities
    // =====================================================================================

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
    {
        if (!ToolData.supportAbility(stack, ToolSettings.SHEAR))
            return InteractionResult.PASS;
        if (isBroken(stack))
        {
            showBrokenMessage(player);
            return InteractionResult.PASS;
        }
        // Vanilla shears logic with our stack: works for every IShearable mob, vanilla or modded.
        return Items.SHEARS.interactLivingEntity(stack, player, target, hand);
    }

    // =====================================================================================
    //  Enchanting / repair / tooltip
    // =====================================================================================

    /*
     * The tools themselves are never enchanted (enchanting table, anvil + book): their enchantments come
     * only from the loaded Enchantment Plate, because loading a plate replaces the tool's enchantments.
     * They are also kept out of the vanilla tool item tags (#minecraft:pickaxes etc.), which is what
     * makes vanilla enchantments apply to an item.
     */

    /** Enchanting table: never accepts the tool. */
    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }

    @Override
    public int getEnchantmentValue()
    {
        return 0;
    }

    /** Anvil: enchanted books can't be put on the tool (use a plate instead). */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book)
    {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate)
    {
        return !tier.isMega() && tier.repairIngredient().test(repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        if (isBroken(stack))
        {
            Item material = switch (tier)
            {
                case STONE -> Items.COBBLESTONE;
                case IRON -> Items.IRON_INGOT;
                case DIAMOND -> Items.DIAMOND;
                case MEGA -> null; // no built-in repair recipe (unbreakable by default)
            };
            tooltip.add(Component.translatable("tooltip.megatools.broken").withStyle(ChatFormatting.RED));
            if (material != null)
                tooltip.add(Component.translatable("tooltip.megatools.repair_hint", new ItemStack(material).getHoverName())
                        .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.megatools.open_gui", Component.keybind(KeyNames.OPEN_GUI)).withStyle(ChatFormatting.GRAY));

        if (tier.storageSlots() > 0)
        {
            int used = ToolData.countStoredItems(stack);
            tooltip.add(Component.translatable("tooltip.megatools.storage", used, tier.storageSlots())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (tier.isMega())  // a modpack can give the Mega Tool durability in megatools-startup.toml
            tooltip.add(Component.translatable(stack.isDamageableItem() ? "tooltip.megatools.mega_breakable" : "tooltip.megatools.mega")
                    .withStyle(ChatFormatting.GOLD));
    }
}
