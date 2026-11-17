package com.thevoos.megatools.items;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.thevoos.megatools.KeyNames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("tooltip.megatools.support_tool").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.megatools.support_tool.usage", Component.keybind(KeyNames.OPEN_GUI)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
