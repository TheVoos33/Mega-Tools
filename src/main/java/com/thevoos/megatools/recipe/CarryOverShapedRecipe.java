package com.thevoos.megatools.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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
    // Kept here because ShapedRecipe's own fields aren't accessible outside its package.
    private final ShapedRecipePattern carryPattern;
    private final ItemStack carryResult;

    public CarryOverShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                                 ItemStack result, boolean showNotification)
    {
        super(group, category, pattern, result, showNotification);
        this.carryPattern = pattern;
        this.carryResult = result;
    }

    /** Vanilla shape match AND the tool's contents fit in the result (never deletes items). */
    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, carryResult);
    }

    /** Vanilla result, then the tool data is copied onto it. */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
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

    // ---- JSON format: identical to minecraft:crafting_shaped ----
    public static final MapCodec<CarryOverShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.carryPattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.carryResult),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
    ).apply(instance, CarryOverShapedRecipe::new));

    // ---- Network format (recipes are synced to clients) ----
    public static final StreamCodec<RegistryFriendlyByteBuf, CarryOverShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
            ShapedRecipePattern.STREAM_CODEC, r -> r.carryPattern,
            ItemStack.STREAM_CODEC, r -> r.carryResult,
            ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
            CarryOverShapedRecipe::new);
}
