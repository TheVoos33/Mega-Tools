package com.thevoos.megatools.items;

import com.thevoos.megatools.KeyNames;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.tool.Ignitables;
import com.thevoos.megatools.tool.LightSources;
import com.thevoos.megatools.tool.MegaToolPoke;
import com.thevoos.megatools.tool.ToolData;
import com.thevoos.megatools.tool.ToolLightManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One class for every tool (Stone / Iron / Diamond / Netherite Paxel, the Mega Tool and its Allthemodium upgrades).
 * Behaviour differences come from {@link ToolTier}.
 *
 * How the vanilla abilities are provided:
 *  - Mining: a custom {@link Tool} data component (pickaxe+axe+shovel+hoe tag, cobweb like a sword,
 *    leaves/wool like shears). Vanilla reads this component for speed and "correct tool" checks.
 *  - Right-click on blocks: we delegate to the vanilla AxeItem/ShovelItem/HoeItem/FlintAndSteelItem
 *    useOn() with OUR stack in the context. Those methods damage the stack in the context (ours),
 *    and NeoForge asks our {@link #canPerformAction} whether the action is allowed - which is where
 *    the GUI toggles are enforced.
 *  - Brushing: we start "using" the item and forward onUseTick() to the vanilla BrushItem.
 *  - Shearing mobs: delegated to the vanilla ShearsItem (NeoForge IShearable: sheep, mooshroom,
 *    snow golem, bogged and modded mobs).
 *  - Shears, brush and flint and steel only work while a Support Tool is in the tool's Support Tool
 *    slot (ToolData#hasSupportTool), each with its own GUI toggle.
 */
public class MultiToolItem extends Item
{
    private final ToolTier tier;
    /** Mining rules without the shears speeds, used while no Support Tool (or Shear toggle) is active. */
    private final Tool toolWithoutShears;

    public MultiToolItem(ToolTier tier, Item.Properties properties)
    {
        super(applyTier(tier, properties));
        this.tier = tier;
        this.toolWithoutShears = createToolComponent(tier, false);
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
     * NeoForge hook called by ItemStack#hurtAndBreak before damage is applied. Trimming the amount so
     * the tool keeps at least 1 durability means vanilla's "durability reached 0 -> destroy the item"
     * step never happens, whatever caused the damage (mining, combat, abilities, other mods).
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken)
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
    //  Item properties: tool component, attributes, durability, fire resistance
    // =====================================================================================

    private static Item.Properties applyTier(ToolTier tier, Item.Properties properties)
    {
        properties.component(DataComponents.TOOL, createToolComponent(tier, tier.supportTools()))
                  .attributes(createAttributes(tier));

        if (tier.durability() > 0)
            properties.durability(tier.durability());   // also forces stack size 1
        else
            properties.stacksTo(1);                       // Mega Tool: no durability = infinite use

        if (tier.isMega())
            properties.fireResistant().rarity(Rarity.EPIC); // survives fire and lava as a dropped item
        else if (tier == ToolTier.NETHERITE)
            properties.fireResistant();                     // like vanilla netherite tools

        return properties;
    }

    /**
     * Mining rules (first matching rule wins for speed; first rule with a drop flag wins for drops).
     */
    private static Tool createToolComponent(ToolTier tier, boolean withShears)
    {
        List<Tool.Rule> rules = new ArrayList<>();
        // Blocks this tier is too weak for (e.g. obsidian for stone) mine but do not drop.
        if (!tier.isMega())
            rules.add(Tool.Rule.deniesDrops(tier.incorrectBlocksForDrops()));
        // Sword ability: cobwebs break fast and drop string.
        rules.add(Tool.Rule.minesAndDrops(List.<Block>of(Blocks.COBWEB), 15.0F));
        // Shears ability: same speeds as vanilla shears (only used while a Support Tool is in, see getDestroySpeed).
        if (withShears)
        {
            rules.add(Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F));
            rules.add(Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0F));
            rules.add(Tool.Rule.overrideSpeed(List.<Block>of(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F));
        }
        // Pickaxe + axe + shovel + hoe.
        rules.add(Tool.Rule.minesAndDrops(ModTags.MINEABLE_WITH_PAXEL, tier.miningSpeed()));
        // Mega Tool takes no damage per block.
        return new Tool(rules, 1.0F, tier.isMega() ? 0 : 1);
    }

    /** Attack damage/speed. The player has 1 base damage, so the modifier is (total - 1). */
    private static ItemAttributeModifiers createAttributes(ToolTier tier)
    {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, tier.attackDamage() - 1.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, tier.attackSpeed() - 4.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // =====================================================================================
    //  Mining / combat
    // =====================================================================================

    /**
     * Only a real switch (other hotbar slot or other item) plays the lower-and-raise equip animation.
     * NeoForge's default re-equips whenever the stack object is replaced, which for these tools happens on
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
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand)
    {
        if (tier.isMega() && hand == InteractionHand.MAIN_HAND && entity instanceof Player player && player.isLocalPlayer())
            MegaToolPoke.onLocalSwing();
        return false;
    }

    /** Shears speeds (leaves, wool, vines) only while the shears ability is active. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state)
    {
        if (tier.isMega() && state.is(ModTags.NOT_MINEABLE))
            return 1.0F; // not the right tool: hand speed
        if (tier.supportTools() && !ToolData.supportAbility(stack, ToolSettings.SHEAR))
            return toolWithoutShears.getMiningSpeed(state);
        return super.getDestroySpeed(stack, state);
    }

    /**
     * The Mega Tools count as the correct tool for every block except #megatools:not_mineable (and the
     * Allthemodium ores their tier is too weak for, see ToolTier#tooWeakFor), so everything else drops.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
    {
        if (isBroken(stack))
            return false; // broken: no tool drops, like an empty hand
        if (tier.isMega())
            return !state.is(ModTags.NOT_MINEABLE) && !tier.tooWeakFor(state);
        return super.isCorrectToolForDrops(stack, state);
    }

    /** Sword behaviour: lose 1 durability per hit (no-op for the unbreakable Mega Tool). */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    /**
     * NeoForge "item abilities": which vanilla tool actions this item can perform.
     * Used by block interactions (stripping, pathing, tilling), shear loot checks, sweep attacks, etc.
     * The right-click abilities honour the GUI toggles stored on the stack.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability)
    {
        if (isBroken(stack))
            return false; // broken: no stripping, shearing, sweeping...
        ToolSettings settings = ToolData.getSettings(stack);

        if (ability == ItemAbilities.AXE_STRIP || ability == ItemAbilities.AXE_SCRAPE || ability == ItemAbilities.AXE_WAX_OFF)
            return settings.has(ToolSettings.STRIP);
        if (ability == ItemAbilities.SHOVEL_FLATTEN)
            return settings.has(ToolSettings.PATH);
        if (ability == ItemAbilities.HOE_TILL)
            return settings.has(ToolSettings.TILL);
        // Support Tool abilities: other mods' blocks and loot tables ask for these too.
        if (ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(ability))
            return ToolData.supportAbility(stack, ToolSettings.SHEAR);
        if (ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(ability))
            return ToolData.supportAbility(stack, ToolSettings.BRUSH);
        if (ItemAbilities.DEFAULT_FLINT_ACTIONS.contains(ability))
            return ToolData.supportAbility(stack, ToolSettings.IGNITE);

        return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
    }

    // =====================================================================================
    //  Right click on a block: brush -> TNT -> torch -> strip -> path/till -> trim -> ignite
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

        boolean support = ToolData.hasSupportTool(stack);

        // 1. Brush: start "using" the item; brushing happens in onUseTick. Only on brushable blocks.
        if (support && settings.has(ToolSettings.BRUSH) && player != null
                && level.getBlockEntity(pos) instanceof BrushableBlockEntity)
        {
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        // 2. Flint and steel on TNT (also modded TNT, like AE2's Tiny TNT): those blocks only accept the real
        //    flint and steel item.
        if (support && settings.has(ToolSettings.IGNITE))
        {
            InteractionResult tnt = tryIgniteTnt(context, stack);
            if (tnt.consumesAction()) return tnt;
        }

        // 3. Place Torch: put one torch from the torch slot on the clicked face. Comes before strip,
        //    path and till, which would otherwise always win on logs, grass and dirt.
        if (settings.has(ToolSettings.PLACE_TORCH))
        {
            InteractionResult torch = tryPlaceTorch(context, stack);
            if (torch.consumesAction()) return torch;
        }

        // 4. Axe: strip logs, scrape/un-wax copper.
        if (settings.has(ToolSettings.STRIP))
        {
            InteractionResult result = Items.IRON_AXE.useOn(context);
            if (result.consumesAction()) return result;
        }

        // 5. Shovel path vs hoe till: both work on grass/dirt, so sneaking picks the hoe first.
        boolean sneaking = player != null && player.isSecondaryUseActive();
        Item first = sneaking ? Items.IRON_HOE : Items.IRON_SHOVEL;
        Item second = sneaking ? Items.IRON_SHOVEL : Items.IRON_HOE;
        InteractionResult result = tryDigAction(first, settings, context);
        if (result.consumesAction()) return result;
        result = tryDigAction(second, settings, context);
        if (result.consumesAction()) return result;

        // 6. Shears: trim growing plants (cave vines, kelp, weeping/twisting vines) so they stop growing,
        //    plus anything other mods add through the SHEARS_TRIM ability.
        if (support && settings.has(ToolSettings.SHEAR))
        {
            InteractionResult trim = Items.SHEARS.useOn(context);
            if (trim.consumesAction()) return trim;
        }

        // 7. Flint and steel: light campfires/candles or place fire.
        if (support && settings.has(ToolSettings.IGNITE))
        {
            InteractionResult ignite = Items.FLINT_AND_STEEL.useOn(context);
            if (ignite.consumesAction()) return ignite;
        }
        return InteractionResult.PASS;
    }

    /**
     * Lights TNT exactly like flint and steel does (TntBlock and modded TNT check for the flint and steel
     * item itself). Works for every block that reacts to catching fire (see Ignitables).
     */
    private static InteractionResult tryIgniteTnt(UseOnContext context, ItemStack tool)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!Ignitables.isTnt(state))
            return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (!level.isClientSide())
        {
            state.onCaughtFire(level, pos, context.getClickedFace(), player);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            if (player != null)
            {
                tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
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
        if (level.isClientSide() || !stack.has(ModDataComponents.TOOL_LIGHT.get()))
            return;
        ItemStack legacy = ToolData.getLegacyLight(stack);
        stack.remove(ModDataComponents.TOOL_LIGHT.get());
        if (legacy.isEmpty())
            return;

        ItemStack current = ToolData.getTorches(stack);
        boolean fits = LightSources.isTorch(legacy)
                && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, legacy))
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
    public int getUseDuration(ItemStack stack, LivingEntity entity)
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        if (isBroken(stack))
        {
            Item material = switch (tier)
            {
                case STONE -> Items.COBBLESTONE;
                case IRON -> Items.IRON_INGOT;
                case DIAMOND -> Items.DIAMOND;
                case NETHERITE -> Items.NETHERITE_INGOT;
                default -> null; // Mega Tools: no built-in repair recipe (unbreakable by default)
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
        // With Allthemodium: which ores this Mega Tool tier can't break yet.
        if (ModItems.ALLTHEMODIUM && tier == ToolTier.MEGA)
            tooltip.add(Component.translatable("tooltip.megatools.mega_tier.mega").withStyle(ChatFormatting.DARK_GRAY));
        else if (ModItems.ALLTHEMODIUM && tier == ToolTier.MEGA_ALLTHEMODIUM)
            tooltip.add(Component.translatable("tooltip.megatools.mega_tier.allthemodium").withStyle(ChatFormatting.DARK_GRAY));
        else if (tier == ToolTier.MEGA_VIBRANIUM || tier == ToolTier.MEGA_UNOBTAINIUM)
            tooltip.add(Component.translatable("tooltip.megatools.mega_tier.all").withStyle(ChatFormatting.DARK_GRAY));
    }
}
