package com.thevoos.megatools.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
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
 *
 * Repair: when the result is the same item as the Mega Tools tool in the grid, the recipe repairs it
 * instead. Each material repairs 1/8 of the tool (see ToolRepair), the tool itself comes out with
 * everything on it unchanged, and the recipe only matches a damaged tool. The default paxel repair
 * recipes (8 materials + the paxel) work this way.
 */
public class CarryOverShapelessRecipe extends ShapelessRecipe implements PreservesToolData
{
    private static final int MAX_INGREDIENTS = 9;

    // Kept here because ShapelessRecipe's own fields aren't accessible.
    private final ItemStackTemplate carryResult;
    private final List<Ingredient> carryIngredients;
    /**
     * The result as a stack, for the checks in matches() (never handed out). Created on first use: recipes are
     * read before item components are bound, so creating it in the constructor crashes the game at startup.
     */
    private ItemStack resultForChecks;

    public CarryOverShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                    ItemStackTemplate result, List<Ingredient> ingredients)
    {
        super(commonInfo, bookInfo, result, ingredients);
        this.carryResult = result;
        this.carryIngredients = ingredients;
    }

    private ItemStack resultForChecks()
    {
        if (resultForChecks == null)
            resultForChecks = carryResult.create();
        return resultForChecks;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        // Repair: the result is the same item as the tool in the grid. Only a damaged tool matches, so
        // materials are never used up on a tool at full durability. Any number of material slots works:
        // each material repairs part of the tool (see ToolRepair).
        ItemStack repairing = ToolCarryOver.findSameTool(input.items(), resultForChecks().getItem());
        if (!repairing.isEmpty())
            return ToolRepair.layout(input.items(), carryIngredients, resultForChecks().getItem()) != null;
        if (!super.matches(input, level))
            return false;
        return ToolCarryOver.canCarryOver(input, resultForChecks());
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        // Repair: hand back the tool itself with the materials' durability added, so everything on it
        // stays (storage, plates, torches, toggles, name, enchantments and other mods' data).
        ToolRepair.Layout repair = ToolRepair.layout(input.items(), carryIngredients, resultForChecks().getItem());
        if (repair != null)
            return repair.repaired();
        ItemStack result = super.assemble(input);
        ToolCarryOver.apply(input, result);
        return result;
    }

    /** Repair: slots whose material the tool doesn't need any more get their item back (see ToolRepair). */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input)
    {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        ToolRepair.Layout repair = ToolRepair.layout(input.items(), carryIngredients, resultForChecks().getItem());
        if (repair != null)
            repair.returnUnused(input.items(), remaining);
        return remaining;
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
