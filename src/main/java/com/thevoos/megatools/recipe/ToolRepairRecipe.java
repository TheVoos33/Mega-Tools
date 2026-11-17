package com.thevoos.megatools.recipe;

import com.google.gson.JsonObject;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
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
    /** Copies a vanilla shaped recipe; category and notification come from the repair defaults (see Serializer). */
    public ToolRepairRecipe(ShapedRecipe parsed, CraftingBookCategory category, boolean showNotification)
    {
        super(parsed.getId(), parsed.getGroup(), category, parsed.getWidth(), parsed.getHeight(),
                parsed.getIngredients(), parsed.getResultItem(RegistryAccess.EMPTY), showNotification);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level)
    {
        return super.matches(input, level) && !findTool(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries)
    {
        ItemStack tool = findTool(input);
        if (tool.isEmpty())
            return ItemStack.EMPTY;
        ItemStack repaired = tool.copyWithCount(1);
        repaired.setDamageValue(0);
        return repaired;
    }

    /** The damaged Mega Tools tool in the grid, or EMPTY. */
    private static ItemStack findTool(CraftingContainer input)
    {
        for (ItemStack stack : input.getItems())
            if (stack.getItem() instanceof MultiToolItem && stack.isDamageableItem() && stack.isDamaged())
                return stack;
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return ModRecipes.TOOL_REPAIR.get();
    }

    /**
     * JSON format: identical to minecraft:crafting_shaped, but "category" defaults to equipment and
     * "show_notification" to false. Network format: vanilla shaped (it includes both values).
     */
    public static class Serializer implements RecipeSerializer<ToolRepairRecipe>
    {
        @Override
        public ToolRepairRecipe fromJson(ResourceLocation id, JsonObject json)
        {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    GsonHelper.getAsString(json, "category", null), CraftingBookCategory.EQUIPMENT);
            boolean notify = GsonHelper.getAsBoolean(json, "show_notification", false);
            return new ToolRepairRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json), category, notify);
        }

        @Override
        public ToolRepairRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf)
        {
            ShapedRecipe parsed = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return new ToolRepairRecipe(parsed, parsed.category(), parsed.showNotification());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ToolRepairRecipe recipe)
        {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
