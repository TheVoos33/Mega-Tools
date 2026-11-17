package com.thevoos.megatools.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A normal shapeless recipe (same JSON as minecraft:crafting_shapeless) that carries a Mega Tools
 * tool's settings, storage, plates and name over to the result. JSON type:
 * "megatools:crafting_shapeless_carry_over".
 */
public class CarryOverShapelessRecipe extends ShapelessRecipe implements PreservesToolData
{
    private static final int MAX_INGREDIENTS = 9;

    // Kept here because ShapelessRecipe's own fields aren't accessible.
    private final ItemStackTemplate carryResult;
    private final List<Ingredient> carryIngredients;
    /** The result as a stack, for the "does everything fit" check in matches() (never handed out). */
    private final ItemStack resultForChecks;

    public CarryOverShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                    ItemStackTemplate result, List<Ingredient> ingredients)
    {
        super(commonInfo, bookInfo, result, ingredients);
        this.carryResult = result;
        this.carryIngredients = ingredients;
        this.resultForChecks = result.create();
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, resultForChecks);
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack result = super.assemble(input);
        ToolCarryOver.apply(input, result);
        return result;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer()
    {
        return ModRecipes.SHAPELESS_CARRY_OVER.get();
    }

    // ---- JSON format: identical to minecraft:crafting_shapeless ----
    public static final MapCodec<CarryOverShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.carryResult),
            Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, MAX_INGREDIENTS)).fieldOf("ingredients").forGetter(r -> r.carryIngredients)
    ).apply(instance, CarryOverShapelessRecipe::new));

    // ---- Network format ----
    public static final StreamCodec<RegistryFriendlyByteBuf, CarryOverShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, r -> r.carryResult,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.carryIngredients,
            CarryOverShapelessRecipe::new);
}
