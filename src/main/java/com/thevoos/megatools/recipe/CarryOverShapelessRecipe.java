package com.thevoos.megatools.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * A normal shapeless recipe (same JSON as minecraft:crafting_shapeless) that carries a Mega Tools
 * tool's settings, storage, plates and name over to the result. JSON type:
 * "megatools:crafting_shapeless_carry_over".
 *
 * Repair: when the result is the same item as the Mega Tools tool in the grid, the recipe repairs it
 * instead. Each material repairs 1/8 of the tool (see ToolRepair), the tool itself comes out with
 * everything on it unchanged, and the recipe only matches a damaged tool. The default paxel repair
 * recipes (8 materials + the paxel) work this way.
 */
public class CarryOverShapelessRecipe extends ShapelessRecipe implements PreservesToolData
{
    private final ItemStack carryResult;

    /** Copies a vanilla shapeless recipe (parsed by vanilla's serializer) into a carry-over recipe. */
    public CarryOverShapelessRecipe(ShapelessRecipe parsed)
    {
        super(parsed.getId(), parsed.getGroup(), parsed.category(), parsed.getResultItem(RegistryAccess.EMPTY), parsed.getIngredients());
        this.carryResult = parsed.getResultItem(RegistryAccess.EMPTY);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level)
    {
        // Repair: the result is the same item as the tool in the grid. Only a damaged tool matches, so
        // materials are never used up on a tool at full durability. Any number of material slots works:
        // each material repairs part of the tool (see ToolRepair).
        ItemStack repairing = ToolCarryOver.findSameTool(input.getItems(), carryResult.getItem());
        if (!repairing.isEmpty())
            return ToolRepair.layout(input.getItems(), getIngredients(), carryResult.getItem()) != null;
        if (!super.matches(input, level))
            return false;
        return ToolCarryOver.canCarryOver(input, carryResult);
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries)
    {
        // Repair: hand back the tool itself with the materials' durability added, so everything on it
        // stays (storage, plates, torches, toggles, name, enchantments and other mods' data).
        ToolRepair.Layout repair = ToolRepair.layout(input.getItems(), getIngredients(), carryResult.getItem());
        if (repair != null)
            return repair.repaired();
        ItemStack result = super.assemble(input, registries);
        ToolCarryOver.apply(input, result);
        return result;
    }

    /** Repair: slots whose material the tool doesn't need any more get their item back (see ToolRepair). */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input)
    {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        ToolRepair.Layout repair = ToolRepair.layout(input.getItems(), getIngredients(), carryResult.getItem());
        if (repair != null)
            repair.returnUnused(input.getItems(), remaining);
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return ModRecipes.SHAPELESS_CARRY_OVER.get();
    }

    /** JSON and network format: identical to minecraft:crafting_shapeless (vanilla's serializer does the work). */
    public static class Serializer implements RecipeSerializer<CarryOverShapelessRecipe>
    {
        @Override
        public CarryOverShapelessRecipe fromJson(ResourceLocation id, JsonObject json)
        {
            return new CarryOverShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json));
        }

        @Override
        public CarryOverShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf)
        {
            return new CarryOverShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CarryOverShapelessRecipe recipe)
        {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buf, recipe);
        }
    }
}
