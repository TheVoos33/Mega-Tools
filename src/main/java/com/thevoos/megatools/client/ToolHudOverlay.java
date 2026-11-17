package com.thevoos.megatools.client;

import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import com.thevoos.megatools.MegaTools;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Auto-swap preview on the LEFT side of the hotbar.
 *
 *  - Holding a Mega Tools tool: every stored tool that matches the targeted block. Index 0 sits
 *    next to the hotbar; more candidates extend to the left and wrap upward if the screen is narrow.
 *    The selected one has a double gold frame (the others a single grey one), so the selection is
 *    readable by shape, not only colour. Above the icons: the swap key and "2/3" when there are
 *    several candidates (Shift + scroll to change).
 *  - Holding a swapped-out tool: one icon of the Mega Tools tool that R swaps back to.
 *
 * The preview stays clear of the off-hand slot (left of the hotbar for right-handed players) and
 * the hotbar attack indicator (left of the hotbar for left-handed players).
 */
public final class ToolHudOverlay
{
    private static final int CELL = 20;      // 16px item + 2px padding each side
    private static final int STRIDE = 22;    // cell + gap
    private static final int GOLD = 0xFFE8B923;
    private static final int GREY = 0xFF8A8A8A;

    private ToolHudOverlay() {}

    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "auto_swap_indicator",
                (gui, graphics, partialTick, width, height) -> render(graphics));
    }

    private static void render(GuiGraphics graphics)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator())
            return;

        ItemStack swapBack = AutoSwapClient.getSwapBackIcon(mc.player);
        List<ItemStack> candidates = swapBack.isEmpty() ? AutoSwapClient.getCandidates() : List.of(swapBack);
        if (candidates.isEmpty())
            return;
        int selected = swapBack.isEmpty() ? AutoSwapClient.getSelectedIndex() : 0;

        // ---- Right edge of the preview: left of the hotbar, skipping whatever vanilla draws there ----
        int centerX = graphics.guiWidth() / 2;
        int rightEdge = centerX - 91 - 4;
        if (mc.player.getMainArm() == HumanoidArm.RIGHT)
        {
            if (!mc.player.getOffhandItem().isEmpty())
                rightEdge = centerX - 91 - 29 - 4;   // off-hand slot
        }
        else if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR)
        {
            rightEdge = centerX - 91 - 22 - 4;       // attack indicator
        }

        // ---- Grid: as many columns as fit, extra rows grow upward ----
        int columns = Math.max(1, Math.min(candidates.size(), (rightEdge - 4) / STRIDE));
        int rows = (candidates.size() + columns - 1) / columns;
        int bottomY = graphics.guiHeight() - CELL - 1;

        for (int i = 0; i < candidates.size(); i++)
        {
            int x = rightEdge - CELL - (i % columns) * STRIDE;
            int y = bottomY - (i / columns) * STRIDE;
            boolean isSelected = i == selected;

            graphics.fill(x, y, x + CELL, y + CELL, 0x90000000);
            if (isSelected)
            {
                graphics.renderOutline(x - 1, y - 1, CELL + 2, CELL + 2, GOLD); // double frame
                graphics.renderOutline(x, y, CELL, CELL, GOLD);
            }
            else
            {
                graphics.renderOutline(x, y, CELL, CELL, GREY);
            }
            graphics.renderItem(candidates.get(i), x + 2, y + 2);
        }

        // ---- Key hint (and "2/3" counter) above the top row, aligned to the hotbar side ----
        Component hint = ModKeyMappings.SWAP_TOOL.getTranslatedKeyMessage().copy();
        if (candidates.size() > 1)
            hint = Component.translatable("hud.megatools.cycle_hint", hint, selected + 1, candidates.size());
        int textY = bottomY - (rows - 1) * STRIDE - 11;
        graphics.drawString(mc.font, hint, rightEdge - mc.font.width(hint), textY, 0xFFFFFF, true);
    }
}
