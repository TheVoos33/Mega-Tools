package com.thevoos.megatools.compat;

import com.thevoos.megatools.client.SwapRulesScreen;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Optional EMI support: items can be dragged from EMI's item list onto the filter slots of the Swap
 * Rules GUI.
 *
 * EMI finds this class through the @EmiEntrypoint annotation and only loads it when EMI is installed;
 * nothing else in Mega Tools refers to it, so the mod runs without EMI. Client only, like EMI's GUI.
 */
@EmiEntrypoint
public class MegaToolsEmiPlugin implements EmiPlugin
{
    @Override
    public void register(EmiRegistry registry)
    {
        registry.addDragDropHandler(SwapRulesScreen.class, new FilterHandler());
    }

    private static class FilterHandler implements EmiDragDropHandler<SwapRulesScreen>
    {
        @Override
        public boolean dropStack(SwapRulesScreen screen, EmiIngredient dragged, int x, int y)
        {
            ItemStack stack = itemOf(dragged);
            if (stack.isEmpty())
                return false;
            List<Rect2i> areas = screen.getFilterAreas();
            for (int i = 0; i < areas.size(); i++)
            {
                if (areas.get(i).contains(x, y))
                {
                    screen.setFilter(i, stack);
                    return true;
                }
            }
            return false;
        }

        /** While an item is dragged: mark the filter slots it can be dropped on. */
        @Override
        public void render(SwapRulesScreen screen, EmiIngredient dragged, GuiGraphics graphics, int mouseX, int mouseY, float delta)
        {
            if (!itemOf(dragged).isEmpty())
                screen.renderDropTargets(graphics);
        }

        /** The dragged item; EMPTY for fluids and other things that can't be a filter. A tag uses its first item. */
        private static ItemStack itemOf(EmiIngredient ingredient)
        {
            for (EmiStack stack : ingredient.getEmiStacks())
            {
                ItemStack item = stack.getItemStack();
                if (!item.isEmpty())
                    return item;
            }
            return ItemStack.EMPTY;
        }
    }
}
