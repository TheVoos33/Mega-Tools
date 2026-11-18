package com.thevoos.megatools.recipe;

import com.thevoos.megatools.MegaTools;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Recipe serializers for the carry-over recipe types. They reuse vanilla's CRAFTING recipe type,
 * so they work in crafting tables and the 2x2 inventory grid.
 */
public final class ModRecipes
{
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MegaTools.MODID);

    public static final RegistryObject<RecipeSerializer<CarryOverShapedRecipe>> SHAPED_CARRY_OVER =
            SERIALIZERS.register("crafting_shaped_carry_over",
                    CarryOverShapedRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<CarryOverShapelessRecipe>> SHAPELESS_CARRY_OVER =
            SERIALIZERS.register("crafting_shapeless_carry_over",
                    CarryOverShapelessRecipe.Serializer::new);

    private ModRecipes() {}

    public static void register(IEventBus eventBus)
    {
        SERIALIZERS.register(eventBus);
    }
}
