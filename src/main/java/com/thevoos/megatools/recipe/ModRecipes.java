package com.thevoos.megatools.recipe;

import com.mojang.serialization.MapCodec;
import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe serializers for the carry-over recipe types. They reuse vanilla's CRAFTING recipe type,
 * so they work in crafting tables, the 2x2 inventory grid and the Crafter.
 */
public final class ModRecipes
{
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MegaTools.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarryOverShapedRecipe>> SHAPED_CARRY_OVER =
            SERIALIZERS.register("crafting_shaped_carry_over",
                    () -> new Serializer<>(CarryOverShapedRecipe.CODEC, CarryOverShapedRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarryOverShapelessRecipe>> SHAPELESS_CARRY_OVER =
            SERIALIZERS.register("crafting_shapeless_carry_over",
                    () -> new Serializer<>(CarryOverShapelessRecipe.CODEC, CarryOverShapelessRecipe.STREAM_CODEC));

    /** Minimal RecipeSerializer: the record accessors codec() / streamCodec() implement the interface. */
    private record Serializer<T extends Recipe<?>>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec)
            implements RecipeSerializer<T> {}

    private ModRecipes() {}

    public static void register(IEventBus eventBus)
    {
        SERIALIZERS.register(eventBus);
    }
}
