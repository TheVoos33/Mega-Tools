package com.thevoos.megatools.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A normal shaped recipe (same JSON as minecraft:crafting_shaped) that carries a Mega Tools
 * tool's settings, storage, plates and name over to the result. JSON type:
 * "megatools:crafting_shaped_carry_over".
 *
 * Because it IS a ShapedRecipe, the recipe book, auto-placement and recipe viewers treat it
 * like any vanilla recipe. Only matches() and assemble() are extended.
 */
public class CarryOverShapedRecipe extends ShapedRecipe implements PreservesToolData
{
    // Kept here because ShapedRecipe#getResultItem needs registry access.
    private final ItemStack carryResult;

    /** Copies a vanilla shaped recipe (parsed by vanilla's serializer) into a carry-over recipe. */
    public CarryOverShapedRecipe(ShapedRecipe parsed)
    {
        super(parsed.getId(), parsed.getGroup(), parsed.category(), parsed.getWidth(), parsed.getHeight(),
                parsed.getIngredients(), parsed.getResultItem(RegistryAccess.EMPTY), parsed.showNotification());
        this.carryResult = parsed.getResultItem(RegistryAccess.EMPTY);
    }

    /** Vanilla shape match AND the tool's contents fit in the result (never deletes items). */
    @Override
    public boolean matches(CraftingContainer input, Level level)
    {
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, carryResult);
    }

    /** Vanilla result, then the tool data is copied onto it. */
    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries)
    {
        ItemStack result = super.assemble(input, registries);
        ToolCarryOver.apply(input, result);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return ModRecipes.SHAPED_CARRY_OVER.get();
    }

    /** JSON and network format: identical to minecraft:crafting_shaped (vanilla's serializer does the work). */
    public static class Serializer implements RecipeSerializer<CarryOverShapedRecipe>
    {
        @Override
        public CarryOverShapedRecipe fromJson(ResourceLocation id, JsonObject json)
        {
            return new CarryOverShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public CarryOverShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf)
        {
            return new CarryOverShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CarryOverShapedRecipe recipe)
        {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
