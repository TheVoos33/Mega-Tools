package com.thevoos.megatools.client;

import com.thevoos.megatools.Config;
import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.MenuStyle;
import com.thevoos.megatools.network.OpenToolGuiPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Shown instead of the tool menu while no menu style is chosen (Config: menuStyle = ASK), which is the
 * first time a player opens the menu of any Mega Tools tool: a picture of each of the two menus, side by
 * side, with a button under it. Clicking a picture or its button saves the choice in
 * megatools-common.toml and opens the tool menu in that style. Esc closes without choosing; the
 * question comes back the next time.
 *
 * The choice can be changed at any time in the config (Mods > Mega Tools > Config, or the file), where
 * ASK brings this screen back.
 */
public class MenuStyleScreen extends Screen
{
    /** One of the two menus: its picture (stored at twice the size it has in the game) and its texts. */
    private enum Option
    {
        CLASSIC(MenuStyle.CLASSIC, "classic", 246, 252),
        ICONS(MenuStyle.ICONS, "icons", 176, 231);

        final MenuStyle style;
        final Identifier texture;
        final Component name;
        final Component description;
        /** Size of the real menu in GUI pixels; the texture is 2x that. */
        final int menuW, menuH;

        Option(MenuStyle style, String key, int menuW, int menuH)
        {
            this.style = style;
            this.texture = MegaTools.id("textures/gui/menu_" + key + ".png");
            this.name = Component.translatable("gui.megatools.menu_style." + key);
            this.description = Component.translatable("gui.megatools.menu_style." + key + ".desc");
            this.menuW = menuW;
            this.menuH = menuH;
        }
    }

    private static final Option[] OPTIONS = Option.values();
    private static final int GAP = 16;
    private static final int TITLE_Y = 10;
    private static final int PREVIEW_Y = 28;
    private static final int BUTTON_H = 20;
    private static final int GOLD = 0xFFE8B923;
    private static final Component HINT = Component.translatable("gui.megatools.menu_style.hint");

    /** Where each picture is drawn (x, y, width, height), set in init(). */
    private final int[][] previews = new int[OPTIONS.length][4];

    public MenuStyleScreen()
    {
        super(Component.translatable("gui.megatools.menu_style.title"));
    }

    @Override
    protected void init()
    {
        // The pictures share one scale, so the two menus keep their real proportions: as large as fits
        // (never larger than the menus really are), leaving room for the title, buttons and the hint.
        int totalW = GAP * (OPTIONS.length - 1);
        int maxH = 0;
        for (Option option : OPTIONS)
        {
            totalW += option.menuW;
            maxH = Math.max(maxH, option.menuH);
        }
        int hintLines = font.split(HINT, width - 20).size();
        int roomH = height - PREVIEW_Y - 6 - BUTTON_H - 6 - hintLines * 10 - 4;
        float scale = Math.min(1.0F, Math.min((width - 20 - GAP * (OPTIONS.length - 1)) / (float) (totalW - GAP * (OPTIONS.length - 1)),
                roomH / (float) maxH));
        scale = Math.max(scale, 0.2F);

        int scaledTotal = GAP * (OPTIONS.length - 1);
        for (Option option : OPTIONS)
            scaledTotal += Math.round(option.menuW * scale);
        int x = (width - scaledTotal) / 2;
        int bottom = PREVIEW_Y + Math.round(maxH * scale);
        for (int i = 0; i < OPTIONS.length; i++)
        {
            Option option = OPTIONS[i];
            int w = Math.round(option.menuW * scale);
            int h = Math.round(option.menuH * scale);
            previews[i] = new int[]{x, bottom - h, w, h}; // bottoms in line, like the buttons under them
            int buttonW = Math.max(w, Math.min(120, font.width(option.name) + 16));
            addRenderableWidget(Button.builder(option.name, b -> choose(option.style))
                    .bounds(x + (w - buttonW) / 2, bottom + 6, buttonW, BUTTON_H)
                    .tooltip(Tooltip.create(option.description))
                    .build());
            x += w + GAP;
        }
    }

    /** Saves the choice for this game and opens the tool menu the player asked for. */
    private void choose(MenuStyle style)
    {
        Config.MENU_STYLE.set(style);
        Config.MENU_STYLE.save();
        onClose();
        if (minecraft != null && minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof MultiToolItem)
            ClientPacketDistributor.sendToServer(OpenToolGuiPayload.INSTANCE);
    }

    private int previewAt(double mouseX, double mouseY)
    {
        for (int i = 0; i < previews.length; i++)
        {
            int[] p = previews[i];
            if (mouseX >= p[0] && mouseX < p[0] + p[2] && mouseY >= p[1] && mouseY < p[1] + p[3])
                return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (super.mouseClicked(event, doubleClick))
            return true;
        int preview = event.button() == 0 ? previewAt(event.x(), event.y()) : -1;
        if (preview < 0)
            return false;
        choose(OPTIONS[preview].style);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick); // buttons (the background is drawn before)
        graphics.centeredText(font, title, width / 2, TITLE_Y, 0xFFFFFFFF);

        int hovered = previewAt(mouseX, mouseY);
        int bottom = 0;
        for (int i = 0; i < OPTIONS.length; i++)
        {
            Option option = OPTIONS[i];
            int[] p = previews[i];
            graphics.blit(RenderPipelines.GUI_TEXTURED, option.texture, p[0], p[1], 0, 0, p[2], p[3], option.menuW * 2, option.menuH * 2, option.menuW * 2, option.menuH * 2);
            if (i == hovered)
                graphics.outline(p[0] - 2, p[1] - 2, p[2] + 4, p[3] + 4, GOLD);
            bottom = Math.max(bottom, p[1] + p[3]);
        }

        int y = bottom + 6 + BUTTON_H + 6;
        for (FormattedCharSequence line : font.split(HINT, width - 20))
        {
            graphics.centeredText(font, line, width / 2, y, 0xFFA0A0A0);
            y += 10;
        }
        if (hovered >= 0)
            graphics.setTooltipForNextFrame(font, font.split(OPTIONS[hovered].description, 200), mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
