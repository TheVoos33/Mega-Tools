package com.thevoos.megatools.recipe;

import com.mojang.serialization.MapCodec;
import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe serializers for the carry-over and repair recipe types. They reuse vanilla's CRAFTING recipe type,
 * so they work in crafting tables, the 2x2 inventory grid and the Crafter.
 */
public final class ModRecipes
{
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MegaTools.MODID);

    // Typed as the vanilla recipe class they extend: ShapedRecipe / ShapelessRecipe#getSerializer must return
    // a serializer of exactly that class. The codecs only ever read and write our own recipe objects.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> SHAPED_CARRY_OVER =
            SERIALIZERS.register("crafting_shaped_carry_over",
                    () -> serializer(CarryOverShapedRecipe.CODEC, CarryOverShapedRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessRecipe>> SHAPELESS_CARRY_OVER =
            SERIALIZERS.register("crafting_shapeless_carry_over",
                    () -> serializer(CarryOverShapelessRecipe.CODEC, CarryOverShapelessRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> TOOL_REPAIR =
            SERIALIZERS.register("tool_repair",
                    () -> serializer(ToolRepairRecipe.CODEC, ToolRepairRecipe.STREAM_CODEC));

    /** A serializer for subclass S, typed as its vanilla base class B (see above). */
    @SuppressWarnings({"unchecked", "deprecation"})
    private static <B extends Recipe<?>, S extends B> RecipeSerializer<B> serializer(MapCodec<S> codec,
                                                                                     StreamCodec<RegistryFriendlyByteBuf, S> streamCodec)
    {
        return new RecipeSerializer<>((MapCodec<B>) (MapCodec<?>) codec, (StreamCodec<RegistryFriendlyByteBuf, B>) (StreamCodec<?, ?>) streamCodec);
    }

    private ModRecipes() {}

    public static void register(IEventBus eventBus)
    {
        SERIALIZERS.register(eventBus);
    }
}
