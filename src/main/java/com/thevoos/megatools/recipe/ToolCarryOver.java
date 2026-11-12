package com.thevoos.megatools.recipe;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Moves a Mega Tools tool's data into the crafted result when that tool is used as a crafting
 * ingredient. Used by the two carry-over recipe types:
 *   megatools:crafting_shaped_carry_over / megatools:crafting_shapeless_carry_over
 *
 * Carried over: ability toggles + active plate (tool_settings), wrench storage, enchantment plates,
 * torches, custom name. (Every tier has a torch slot, so the torches never block a recipe.) Durability is NOT carried over: the result is a fresh tool.
 *
 * Nothing is ever deleted:
 *  - If the result can't hold everything (fewer storage slots than stored tools, or no plate slots
 *    while plates are installed), the recipe simply doesn't match. Empty the tool first.
 *  - If two tool ingredients both hold items, the recipe doesn't match (no silent merging).
 *  - For any OTHER recipe (vanilla, other mods) that consumes a tool, {@link #onItemCrafted}
 *    returns the stored tools and plates to the player.
 */
public final class ToolCarryOver
{
    private ToolCarryOver() {}

    /** The tool ingredient whose data is carried over (first one holding items, else first tool), or EMPTY. */
    private static ItemStack findSource(CraftingInput input)
    {
        ItemStack firstTool = ItemStack.EMPTY;
        for (ItemStack stack : input.items())
        {
            if (!(stack.getItem() instanceof MultiToolItem))
                continue;
            if (hasContents(stack))
                return stack;
            if (firstTool.isEmpty())
                firstTool = stack;
        }
        return firstTool;
    }

    /** True if the carry-over is possible without losing any items. Called from matches(). */
    public static boolean canCarryOver(CraftingInput input, ItemStack result)
    {
        int toolsWithContents = 0;
        for (ItemStack stack : input.items())
            if (stack.getItem() instanceof MultiToolItem && hasContents(stack))
                toolsWithContents++;
        if (toolsWithContents == 0)
            return true;
        if (toolsWithContents > 1)
            return false;

        ItemStack source = findSource(input);
        if (!(result.getItem() instanceof MultiToolItem resultTool))
            return false;

        ToolTier resultTier = resultTool.getTier();
        if (storedItems(source).size() > resultTier.storageSlots())
            return false;
        return resultTier.hasEnchantPlates() || ToolData.getPlates(source).stream().allMatch(ItemStack::isEmpty);
    }

    /** Copies the data onto the freshly assembled result. Called from assemble(). */
    public static void apply(CraftingInput input, ItemStack result)
    {
        ItemStack source = findSource(input);
        if (source.isEmpty() || !(result.getItem() instanceof MultiToolItem resultTool))
            return;
        ToolTier resultTier = resultTool.getTier();

        // Settings (toggles + which plate is loaded) and custom name
        if (source.has(ModDataComponents.TOOL_SETTINGS.get()))
            ToolData.setSettings(result, ToolData.getSettings(source));
        if (source.has(DataComponents.CUSTOM_NAME))
            result.set(DataComponents.CUSTOM_NAME, source.get(DataComponents.CUSTOM_NAME));

        // Wrench storage: keep slot positions when they fit, otherwise pack into the first slots
        if (resultTier.storageSlots() > 0 && sourceTier(source).storageSlots() > 0)
        {
            NonNullList<ItemStack> sourceStorage = ToolData.getStorage(source, sourceTier(source).storageSlots());
            NonNullList<ItemStack> target = NonNullList.withSize(resultTier.storageSlots(), ItemStack.EMPTY);
            boolean positionsFit = true;
            for (int i = resultTier.storageSlots(); i < sourceStorage.size(); i++)
                if (!sourceStorage.get(i).isEmpty()) positionsFit = false;

            if (positionsFit)
            {
                for (int i = 0; i < Math.min(sourceStorage.size(), target.size()); i++)
                    target.set(i, sourceStorage.get(i).copy());
            }
            else
            {
                List<ItemStack> items = storedItems(source);
                for (int i = 0; i < items.size(); i++)
                    target.set(i, items.get(i).copy());
            }
            ToolData.setStorage(result, target);
        }

        // Torch slot (all tiers have one)
        ToolData.setTorches(result, ToolData.getTorches(source));

        // Enchantment plates (then load the active one onto the new tool)
        if (resultTier.hasEnchantPlates())
        {
            ToolData.setPlates(result, ToolData.getPlates(source));
            ToolData.applyActivePlate(result);
        }
    }

    // =====================================================================================
    //  Safety net for recipes that are NOT carry-over recipes
    // =====================================================================================

    /**
     * Game bus, fired once per craft BEFORE the ingredients are consumed. If a non-carry-over
     * recipe consumes a Mega Tools tool, its stored tools and plates go back to the player.
     */
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getInventory() instanceof CraftingContainer grid))
            return;

        CraftingInput input = grid.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level());
        if (recipe.isEmpty())
            return;
        CraftingRecipe value = recipe.get().value();

        // Carry-over and repair recipes already moved everything into the result.
        if (value instanceof PreservesToolData)
            return;
        // If the recipe hands the tool back (a "tool stays in the grid" recipe), nothing is lost.
        if (value.getRemainingItems(input).stream().anyMatch(s -> s.getItem() instanceof MultiToolItem))
            return;

        boolean refunded = false;
        for (int i = 0; i < grid.getContainerSize(); i++)
        {
            ItemStack stack = grid.getItem(i);
            if (!(stack.getItem() instanceof MultiToolItem) || !hasContents(stack))
                continue;

            List<ItemStack> refund = new ArrayList<>(storedItems(stack));
            for (ItemStack plate : ToolData.getPlates(stack))
                if (!plate.isEmpty()) refund.add(plate);
            ItemStack torches = ToolData.getTorches(stack);
            if (!torches.isEmpty())
                refund.add(torches);
            for (ItemStack item : refund)
            {
                // Hand back in normal-sized stacks (up to 256 torches would be an invalid over-stack).
                int left = item.getCount();
                while (left > 0)
                {
                    ItemStack copy = item.copyWithCount(Math.min(left, item.getMaxStackSize()));
                    left -= copy.getCount();
                    if (!player.getInventory().add(copy)) // add() shrinks copy by what it inserted
                        player.drop(copy, false);         // drop only the leftover
                }
            }

            // Clear the consumed tool so its contents can never exist twice.
            stack.remove(ModDataComponents.TOOL_STORAGE.get());
            stack.remove(ModDataComponents.ENCHANTMENT_PLATES.get());
            stack.remove(ModDataComponents.TORCHES.get());
            refunded = true;
        }
        if (refunded)
            player.displayClientMessage(Component.translatable("message.megatools.contents_returned"), true);
    }

    // =====================================================================================
    //  Helpers
    // =====================================================================================

    private static ToolTier sourceTier(ItemStack tool)
    {
        return ((MultiToolItem) tool.getItem()).getTier();
    }

    private static List<ItemStack> storedItems(ItemStack tool)
    {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : ToolData.getStorage(tool, sourceTier(tool).storageSlots()))
            if (!stack.isEmpty()) items.add(stack);
        return items;
    }

    private static boolean hasContents(ItemStack tool)
    {
        return !storedItems(tool).isEmpty()
                || ToolData.getPlates(tool).stream().anyMatch(s -> !s.isEmpty())
                || !ToolData.getTorches(tool).isEmpty();
    }
}
