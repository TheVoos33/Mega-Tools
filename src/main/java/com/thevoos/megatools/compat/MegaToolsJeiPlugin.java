package com.thevoos.megatools.compat;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.client.SwapRulesScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Optional JEI support: items can be dragged from JEI's item list onto the filter slots of the Swap
 * Rules GUI (and Shift-clicked there to add them as a filter).
 *
 * JEI finds this class through the @JeiPlugin annotation and only loads it when JEI is installed;
 * nothing else in Mega Tools refers to it, so the mod runs without JEI. Client only, like JEI's GUI.
 */
@JeiPlugin
public class MegaToolsJeiPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return MegaTools.id("jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGhostIngredientHandler(SwapRulesScreen.class, new FilterHandler());
    }

    private static class FilterHandler implements IGhostIngredientHandler<SwapRulesScreen>
    {
        @Override
        public <I> List<Target<I>> getTargetsTyped(SwapRulesScreen screen, ITypedIngredient<I> ingredient, boolean doStart)
        {
            Optional<ItemStack> stack = ingredient.getItemStack(); // fluids etc. can't be filters
            if (stack.isEmpty())
                return List.of();

            List<Target<I>> targets = new ArrayList<>();
            List<Rect2i> areas = screen.getFilterAreas();
            for (int i = 0; i < areas.size(); i++)
            {
                final int index = i;
                final Rect2i area = areas.get(i);
                targets.add(new Target<>()
                {
                    @Override
                    public Rect2i getArea()
                    {
                        return area;
                    }

                    @Override
                    public void accept(I dropped)
                    {
                        screen.setFilter(index, stack.get());
                    }
                });
            }
            return targets;
        }

        /** Shift-click in JEI's list: add as a filter of the selected tool. */
        @Override
        public <I> boolean quickMove(SwapRulesScreen screen, ITypedIngredient<I> ingredient)
        {
            Optional<ItemStack> stack = ingredient.getItemStack();
            if (stack.isEmpty() || screen.getFilterAreas().isEmpty())
                return false;
            screen.addFilter(stack.get());
            return true;
        }

        @Override
        public void onComplete()
        {
        }
    }
}
