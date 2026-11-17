package com.thevoos.megatools.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Full repair: "megatools:tool_repair", same JSON as minecraft:crafting_shaped.
 *
 * The result is the INPUT tool itself, copied, with its damage reset to 0, so every piece of data
 * stays exactly as it was: wrench storage, plates, light source, toggles, name, enchantments and
 * anything other mods stored on it. The "result" in the JSON is only what the recipe book shows.
 *
 * Only matches a tool that is actually damaged, so materials are never wasted on a full tool.
 */
public class ToolRepairRecipe extends ShapedRecipe implements PreservesToolData
{
    private final ItemStackTemplate displayResult;

    public ToolRepairRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                            ShapedRecipePattern pattern, ItemStackTemplate result)
    {
        super(commonInfo, bookInfo, pattern, result);
        this.displayResult = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return super.matches(input, level) && !findTool(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack tool = findTool(input);
        if (tool.isEmpty())
            return ItemStack.EMPTY;
        ItemStack repaired = tool.copyWithCount(1);
        repaired.setDamageValue(0);
        return repaired;
    }

    /** The damaged Mega Tools tool in the grid, or EMPTY. */
    private static ItemStack findTool(CraftingInput input)
    {
        for (ItemStack stack : input.items())
            if (stack.getItem() instanceof MultiToolItem && stack.isDamageableItem() && stack.isDamaged())
                return stack;
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer()
    {
        return ModRecipes.TOOL_REPAIR.get();
    }

    // ---- JSON format: identical to minecraft:crafting_shaped ----
    // (Defaults differ from vanilla: no "new recipe" toast, equipment tab.)
    public static final MapCodec<ToolRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("show_notification", false).xmap(Recipe.CommonInfo::new, Recipe.CommonInfo::showNotification)
                    .forGetter(r -> r.commonInfo),
            RecordCodecBuilder.<CraftingRecipe.CraftingBookInfo>mapCodec(book -> book.group(
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.EQUIPMENT).forGetter(CraftingRecipe.CraftingBookInfo::category),
                    Codec.STRING.optionalFieldOf("group", "").forGetter(CraftingRecipe.CraftingBookInfo::group)
            ).apply(book, CraftingRecipe.CraftingBookInfo::new)).forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.displayResult)
    ).apply(instance, ToolRepairRecipe::new));

    // ---- Network format ----
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolRepairRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.displayResult,
            ToolRepairRecipe::new);
}
