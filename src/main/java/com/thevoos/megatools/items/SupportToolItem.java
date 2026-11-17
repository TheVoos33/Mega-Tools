package com.thevoos.megatools.items;

import com.thevoos.megatools.KeyNames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Support Tool - shears, a brush and flint and steel in one item (crafted from those three).
 *
 * It does nothing on its own: put it in the Support Tool slot of any paxel or the Mega Tool (open GUI key) to give that tool the Shear, Brush and Ignite abilities and their GUI toggles.
 * Taking it out removes them again. See ToolData#hasSupportTool.
 */
public class SupportToolItem extends Item
{
    public SupportToolItem(Item.Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    @SuppressWarnings("deprecation") // the vanilla way for items to add tooltip lines
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        tooltip.accept(Component.translatable("tooltip.megatools.support_tool").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.megatools.support_tool.usage", Component.keybind(KeyNames.OPEN_GUI)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
