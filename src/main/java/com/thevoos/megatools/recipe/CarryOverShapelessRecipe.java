package com.thevoos.megatools.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
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
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, carryResult);
    }

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
