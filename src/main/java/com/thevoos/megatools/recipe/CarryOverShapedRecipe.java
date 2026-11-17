package com.thevoos.megatools.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
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
    // Kept here because ShapedRecipe's own result field isn't accessible.
    private final ItemStackTemplate carryResult;
    /** The result as a stack, for the "does everything fit" check in matches() (never handed out). */
    private final ItemStack resultForChecks;

    public CarryOverShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                 ShapedRecipePattern pattern, ItemStackTemplate result)
    {
        super(commonInfo, bookInfo, pattern, result);
        this.carryResult = result;
        this.resultForChecks = result.create();
    }

    /** Vanilla shape match AND the tool's contents fit in the result (never deletes items). */
    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, resultForChecks);
    }

    /** Vanilla result, then the tool data is copied onto it. */
    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack result = super.assemble(input);
        ToolCarryOver.apply(input, result);
        return result;
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer()
    {
        return ModRecipes.SHAPED_CARRY_OVER.get();
    }

    // ---- JSON format: identical to minecraft:crafting_shaped ----
    public static final MapCodec<CarryOverShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.carryResult)
    ).apply(instance, CarryOverShapedRecipe::new));

    // ---- Network format (recipes are synced to clients) ----
    public static final StreamCodec<RegistryFriendlyByteBuf, CarryOverShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.carryResult,
            CarryOverShapedRecipe::new);
}
