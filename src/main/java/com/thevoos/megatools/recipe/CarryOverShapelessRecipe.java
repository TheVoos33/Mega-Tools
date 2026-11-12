package com.thevoos.megatools.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
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
    private static final int MAX_INGREDIENTS = 9;

    private final ItemStack carryResult;

    public CarryOverShapelessRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients)
    {
        super(group, category, result, ingredients);
        this.carryResult = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return super.matches(input, level) && ToolCarryOver.canCarryOver(input, carryResult);
    }

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
        return ModRecipes.SHAPELESS_CARRY_OVER.get();
    }

    // ---- JSON format: identical to minecraft:crafting_shapeless ----
    public static final MapCodec<CarryOverShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.carryResult),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(
                    CarryOverShapelessRecipe::validateIngredients,
                    DataResult::success
            ).forGetter(ShapelessRecipe::getIngredients)
    ).apply(instance, CarryOverShapelessRecipe::new));

    private static DataResult<NonNullList<Ingredient>> validateIngredients(java.util.List<Ingredient> list)
    {
        Ingredient[] ingredients = list.stream().filter(i -> !i.isEmpty()).toArray(Ingredient[]::new);
        if (ingredients.length == 0)
            return DataResult.error(() -> "No ingredients for shapeless carry-over recipe");
        if (ingredients.length > MAX_INGREDIENTS)
            return DataResult.error(() -> "Too many ingredients for shapeless carry-over recipe (max " + MAX_INGREDIENTS + ")");
        return DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
    }

    // ---- Network format ----
    public static final StreamCodec<RegistryFriendlyByteBuf, CarryOverShapelessRecipe> STREAM_CODEC =
            StreamCodec.of(CarryOverShapelessRecipe::toNetwork, CarryOverShapelessRecipe::fromNetwork);

    private static void toNetwork(RegistryFriendlyByteBuf buf, CarryOverShapelessRecipe recipe)
    {
        ByteBufCodecs.STRING_UTF8.encode(buf, recipe.getGroup());
        CraftingBookCategory.STREAM_CODEC.encode(buf, recipe.category());
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients())
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
        ItemStack.STREAM_CODEC.encode(buf, recipe.carryResult);
    }

    private static CarryOverShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buf)
    {
        String group = ByteBufCodecs.STRING_UTF8.decode(buf);
        CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buf);
        int count = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
        for (int i = 0; i < count; i++)
            ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        return new CarryOverShapelessRecipe(group, category, result, ingredients);
    }
}
