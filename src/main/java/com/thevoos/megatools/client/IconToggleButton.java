package com.thevoos.megatools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A toggle of the icon menu (see ToolScreen): an 18x18 button, the size of a slot, showing an item
 * instead of a label.
 *
 *  - ON:  the icon as it is, in a green frame.
 *  - OFF: the icon dimmed and struck through, in a red frame. Shape and brightness carry the state too,
 *         so colour is never the only cue (colourblind friendly).
 *  - Not available (inactive, e.g. Shear without a Support Tool): greyed out, no frame colour.
 *
 * The button itself is vanilla's button sprite, so resource packs restyle it like every other button.
 * The name is the widget's message: never drawn, but read by the narrator.
 */
public class IconToggleButton extends Button
{
    public static final int SIZE = 18;
    private static final int ON_COLOR = 0xFF40DC40;
    private static final int OFF_COLOR = 0xFFE03030;
    /** Laid over the icon of a toggle that is off or not available. */
    private static final int DIM = 0x80282828;
    private static final int HOVER = 0x40FFFFFF;

    private final ItemStack icon;
    private boolean on = true;

    public IconToggleButton(int x, int y, ItemStack icon, Component name, OnPress onPress)
    {
        super(x, y, SIZE, SIZE, name, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    public void setOn(boolean on)
    {
        this.on = on;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        int x = getX();
        int y = getY();
        g.blitSprite(SPRITES.get(active, false), x, y, width, height);
        if (active)
            g.renderOutline(x, y, width, height, on ? ON_COLOR : OFF_COLOR);
        g.renderFakeItem(icon, x + 1, y + 1);

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F); // above the item, which renders at a raised depth
        if (!on || !active)
            g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, DIM);
        if (!on && active)
            for (int i = 0; i < 12; i++) // struck through, top left to bottom right: across the way tools are drawn
                g.fill(x + 3 + i, y + 3 + i, x + 5 + i, y + 4 + i, OFF_COLOR);
        if (active && isHighlighted())
            g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, HOVER);
        g.pose().popPose();
    }

    /**
     * Under the mouse, or reached with Tab. Not merely focused: a click also focuses the button, and the
     * highlight would then stay on the last clicked button until another one is clicked.
     */
    private boolean isHighlighted()
    {
        return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
    }
}
