package com.thevoos.megatools.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Repair recipes: a megatools:crafting_shapeless_carry_over recipe whose result is the same item as the tool
 * in the grid (see CarryOverShapelessRecipe). The default ones list 8 materials + the paxel.
 *
 * Each material repairs 1/8 of the tool's durability (1/n for a recipe with n materials). Like any crafting,
 * a craft takes one material from each slot that holds some: materials in two slots repair 1/4, eight
 * materials around the tool repair it fully, and a stack of 8 in one slot repairs 1/8 per craft. Slots whose
 * material the tool doesn't need any more (it is almost repaired) are handed back by getRemainingItems, so a
 * craft never uses more materials than it repairs.
 *
 * Nothing here depends on who crafts, so autocrafters and other mods' crafting tables work the same way.
 */
public final class ToolRepair
{
    private ToolRepair() {}

    /**
     * A repair laid out in a crafting grid.
     *
     * @param tool          the damaged tool (the grid's own stack: read only)
     * @param materials     the number of materials in the recipe (8 for the default recipes)
     * @param materialSlots grid indexes of the slots holding materials, in grid order
     */
    public record Layout(ItemStack tool, int materials, List<Integer> materialSlots)
    {
        /** Durability that {@code count} materials repair (rounded up, so all of them always repair it fully). */
        private int repairedBy(int count)
        {
            return (int) Math.ceil((double) tool.getMaxDamage() * count / materials);
        }

        /** Materials this craft uses: one per slot, but only as many as the tool still needs. */
        public int used()
        {
            int needed = (int) Math.ceil((double) tool.getDamageValue() * materials / tool.getMaxDamage());
            return Math.min(materialSlots.size(), Math.max(1, needed));
        }

        /** The tool itself with the used materials' durability back; everything else on it is kept. */
        public ItemStack repaired()
        {
            ItemStack repaired = tool.copyWithCount(1);
            repaired.setDamageValue(Math.max(0, tool.getDamageValue() - repairedBy(used())));
            return repaired;
        }

        /** Hands back one material for every slot the craft doesn't use (vanilla takes one from each slot). */
        public void returnUnused(List<ItemStack> grid, NonNullList<ItemStack> remaining)
        {
            for (int i = used(); i < materialSlots.size(); i++)
            {
                int slot = materialSlots.get(i);
                remaining.set(slot, grid.get(slot).copyWithCount(1));
            }
        }
    }

    /**
     * The repair in this grid, or null if it isn't one: exactly one damaged {@code tool}, and 1 to n slots of
     * materials (any stack size), each accepted by every material ingredient (the materials are interchangeable).
     */
    public static @Nullable Layout layout(List<ItemStack> grid, List<Ingredient> ingredients, Item tool)
    {
        List<Ingredient> materials = materials(ingredients, tool);
        if (materials == null || materials.isEmpty())
            return null;
        ItemStack target = ItemStack.EMPTY;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < grid.size(); i++)
        {
            ItemStack stack = grid.get(i);
            if (stack.isEmpty())
                continue;
            if (stack.is(tool))
            {
                if (!target.isEmpty())
                    return null; // two tools
                target = stack;
                continue;
            }
            for (Ingredient material : materials)
                if (!material.test(stack))
                    return null;
            slots.add(i);
        }
        if (target.isEmpty() || !target.isDamaged() || target.getMaxDamage() <= 0 || slots.isEmpty() || slots.size() > materials.size())
            return null;
        return new Layout(target, materials.size(), List.copyOf(slots));
    }

    /** The recipe's material ingredients: all of them except the one for the tool, or null if none is for the tool. */
    private static @Nullable List<Ingredient> materials(List<Ingredient> ingredients, Item tool)
    {
        List<Ingredient> materials = new ArrayList<>(ingredients);
        ItemStack toolStack = new ItemStack(tool);
        for (int i = 0; i < materials.size(); i++)
            if (materials.get(i).test(toolStack))
            {
                materials.remove(i);
                return materials;
            }
        return null;
    }
}
