package com.thevoos.megatools.client;

import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.menu.GuiLayout;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.network.RenameToolPayload;
import com.thevoos.megatools.network.SwapToolPayload;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The tool GUI (Ctrl + R).
 *
 *  - Title bar: an EditBox showing the tool's name. Click it to rename; Enter, clicking elsewhere
 *    or closing the screen sends the new name to the server.
 *  - Toggle buttons: "[check] Strip" (green) = enabled, "[cross] Strip" (red) = disabled. The check/cross
 *    symbol carries the state on its own, so colour is never the only cue (colourblind friendly).
 *    Row 1 = paxel abilities, row 2 = support tools, row 3 = Delayed Break (Mega Tool only).
 *  - Right column: plate slots (the active plate is outlined in gold), then the light slot.
 *    Empty plate/light slots show a faded placeholder icon.
 *  - Wrench storage at the bottom of the tool section, then the player inventory.
 *    Hover a stored tool and press R to close the GUI and swap straight to it.
 *
 * Everything is drawn with fills (no texture file needed). The title bar colour comes from
 * ToolTier#accentColor, and the layout height depends on the tier, so every tool's GUI is unique.
 */
public class ToolScreen extends AbstractContainerScreen<ToolMenu>
{
    // Index i = button id = ToolSettings flag (1 << i)
    private static final String[] TOGGLE_KEYS = {"strip", "path", "till", "shear", "brush", "ignite", "delayed_break", "place_torch"};
    private static final String ON_SYMBOL = "\u2714 ";  // heavy check mark
    private static final String OFF_SYMBOL = "\u2716 "; // heavy multiplication x
    private static final int TOGGLE_W = 44;
    private static final int TOGGLE_H = 16;
    private static final int LABEL_COLOR = 0x404040;

    // Created per screen (not static) so they are never built before items are registered.
    private final ItemStack lightPlaceholder = new ItemStack(Items.TORCH);
    private final ItemStack platePlaceholder = new ItemStack(ModItems.ENCHANTMENT_PLATE.get());

    private final Button[] toggleButtons = new Button[TOGGLE_KEYS.length];
    private EditBox nameField;
    private String lastSentName = "";
    private int lastFlags = -1;

    public ToolScreen(ToolMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = GuiLayout.WIDTH;
        this.imageHeight = menu.getLayout().height();
    }

    // =====================================================================================
    //  Widgets
    // =====================================================================================

    @Override
    protected void init()
    {
        super.init(); // computes leftPos / topPos

        // ---- Renamable title ----
        String currentName = menu.getTool().getHoverName().getString();
        nameField = new EditBox(this.font, leftPos + 10, topPos + 6, imageWidth - 20, 10,
                Component.translatable("gui.megatools.rename"));
        nameField.setBordered(false);
        nameField.setMaxLength(RenameToolPayload.MAX_LENGTH);
        nameField.setTextColor(0xFFFFFF);
        nameField.setValue(currentName);
        lastSentName = currentName;
        addRenderableWidget(nameField);

        // ---- Ability toggles ----
        // Ability toggles 0-5 fill a 3-wide grid. The last row holds Place Torch (all tools), shared
        // with Delayed Break on the Mega Tool (two half-width buttons).
        ToolTier tier = menu.getTier();
        int lastRowY = topPos + 18 + (tier.toggleRows() - 1) * 18;
        int lastRowWidth = TOGGLE_W * 3 + 4;
        int lastRowCount = tier.hasToggle(6) ? 2 : 1;
        int lastRowButtonW = (lastRowWidth - (lastRowCount - 1) * 2) / lastRowCount;
        int lastRowIndex = 0;
        for (int i = 0; i < TOGGLE_KEYS.length; i++)
        {
            if (!tier.hasToggle(i))
                continue;
            final int buttonId = i;
            int x, y, width;
            if (i < 6)
            {
                x = leftPos + 8 + (i % 3) * (TOGGLE_W + 2);
                y = topPos + 18 + (i / 3) * 18;
                width = TOGGLE_W;
            }
            else
            {
                x = leftPos + 8 + lastRowIndex++ * (lastRowButtonW + 2);
                y = lastRowY;
                width = lastRowButtonW;
            }
            toggleButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> onToggle(buttonId))
                    .bounds(x, y, width, TOGGLE_H)
                    .build());
        }
        lastFlags = -1;
        refreshToggles();
    }

    /** Sends a vanilla "menu button" packet; the server flips the flag in ToolMenu#clickMenuButton. */
    private void onToggle(int buttonId)
    {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    /** Re-labels the buttons when the synced settings change (symbol + colour + tooltip). */
    private void refreshToggles()
    {
        int flags = ToolData.getSettings(menu.getTool()).flags();
        if (flags == lastFlags)
            return;
        lastFlags = flags;

        for (int i = 0; i < toggleButtons.length; i++)
        {
            Button button = toggleButtons[i];
            if (button == null)
                continue;
            boolean enabled = (flags & (1 << i)) != 0; // button i <-> ToolSettings flag (1 << i)
            String key = "gui.megatools.toggle." + TOGGLE_KEYS[i];
            button.setMessage(Component.literal(enabled ? ON_SYMBOL : OFF_SYMBOL)
                    .append(Component.translatable(key))
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
            button.setTooltip(Tooltip.create(Component.translatable(key + ".desc",
                    Component.translatable(enabled ? "gui.megatools.enabled" : "gui.megatools.disabled"))));
        }
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        refreshToggles();
    }

    // =====================================================================================
    //  Renaming
    // =====================================================================================

    private void commitRename()
    {
        String value = nameField.getValue().trim();
        if (!value.equals(lastSentName))
        {
            lastSentName = value;
            PacketDistributor.sendToServer(new RenameToolPayload(value));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // R while hovering a stored tool. Normally already handled earlier by
        // ClientEvents#onScreenKeyPressed (so it wins over JEI); this is a fallback.
        if (tryQuickSwap(keyCode, scanCode))
            return true;

        if (nameField.isFocused())
        {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
            {
                commitRename();
                nameField.setFocused(false);
                return true;
            }
            // Swallow every other key (except Escape) so typing "e" or "1" doesn't close the
            // screen or move items.
            if (keyCode != GLFW.GLFW_KEY_ESCAPE)
                return nameField.keyPressed(keyCode, scanCode, modifiers) || nameField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Quick swap from the GUI. The screen is closed BEFORE the swap packet is sent: packets arrive in
     * order, so the server closes the menu first and the stored tool can't also be taken out of
     * the still-open storage slot. (Closing also sends any pending rename, so the swapped-out
     * Mega Tools tool keeps its new name.)
     *
     * Returns true only if the key was the swap key AND a stored tool is hovered, so the key is
     * consumed in that case only and other mods (e.g. JEI's R = show recipe) work everywhere else.
     */
    public boolean tryQuickSwap(int keyCode, int scanCode)
    {
        if (nameField.isFocused())
            return false; // typing "r" in the name field
        if (!ModKeyMappings.SWAP_TOOL.matches(keyCode, scanCode) || !ModKeyMappings.modifierMatches(ModKeyMappings.SWAP_TOOL))
            return false;
        if (!(hoveredSlot instanceof ToolMenu.StorageSlot storageSlot) || !storageSlot.hasItem())
            return false;

        int storageIndex = storageSlot.getContainerSlot();
        onClose();
        PacketDistributor.sendToServer(new SwapToolPayload(storageIndex));
        return true;
    }

    /** Adds "Press R to swap to this tool" under the tooltip of stored tools. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack)
    {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot instanceof ToolMenu.StorageSlot)
        {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add(Component.translatable("gui.megatools.quick_swap_hint",
                    ModKeyMappings.SWAP_TOOL.getTranslatedKeyMessage()).withStyle(ChatFormatting.GOLD));
        }
        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // Clicking anywhere else confirms the rename.
        if (nameField.isFocused() && !nameField.isMouseOver(mouseX, mouseY))
        {
            commitRename();
            nameField.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed()
    {
        commitRename(); // closing the screen (Esc / E) also confirms the rename
        super.removed();
    }

    // =====================================================================================
    //  Rendering
    // =====================================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY); // item tooltips

        // Hint tooltips for empty plate slots and the title.
        if (hoveredSlot instanceof ToolMenu.LightSlot lightSlot && !lightSlot.hasItem())
        {
            graphics.renderTooltip(font, Component.translatable("gui.megatools.light_slot"), mouseX, mouseY);
        }
        else if (hoveredSlot instanceof ToolMenu.PlateSlot plateSlot && !plateSlot.hasItem())
        {
            boolean primary = plateSlot.getContainerSlot() == ToolData.PRIMARY;
            graphics.renderTooltip(font, Component.translatable(primary ? "gui.megatools.plate.primary" : "gui.megatools.plate.secondary"),
                    mouseX, mouseY);
        }
        else if (!nameField.isFocused() && nameField.isMouseOver(mouseX, mouseY))
        {
            graphics.renderTooltip(font, Component.translatable("gui.megatools.rename_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = leftPos;
        int y = topPos;
        GuiLayout layout = menu.getLayout();

        // Panel + tier-coloured title bar
        drawPanel(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x + 6, y + 4, x + imageWidth - 6, y + 16, menu.getTier().accentColor());

        // Slot frames for every slot in the menu, plus faded placeholder icons in empty special slots
        for (Slot slot : menu.slots)
        {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
            if (!slot.hasItem())
            {
                if (slot instanceof ToolMenu.LightSlot)
                    drawGhostItem(graphics, lightPlaceholder, x + slot.x, y + slot.y);
                else if (slot instanceof ToolMenu.PlateSlot)
                    drawGhostItem(graphics, platePlaceholder, x + slot.x, y + slot.y);
            }
        }

        // Gold outline around the currently loaded enchantment plate slot
        if (menu.getTier().hasEnchantPlates())
        {
            int active = ToolData.getSettings(menu.getTool()).activePlate();
            int plateY = active == ToolData.PRIMARY ? layout.plateY0() : layout.plateY1();
            graphics.renderOutline(x + layout.plateX() - 2, y + plateY - 2, 20, 20, 0xFFE8B923);
        }
    }

    /** Section labels. The title is the EditBox, so the default title/inventory labels are replaced. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        GuiLayout layout = menu.getLayout();
        if (layout.storageRows() > 0)
            graphics.drawString(font, Component.translatable("gui.megatools.tool_storage"), 8, layout.storageLabelY(), LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, 8, layout.invLabelY(), LABEL_COLOR, false);
    }

    /** Vanilla-style bevelled grey panel. */
    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);          // top highlight
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);          // left highlight
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);  // bottom shadow
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF555555);  // right shadow
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
    }

    /** Draws an item, then a translucent slot-coloured layer over it so it reads as a placeholder. */
    private static void drawGhostItem(GuiGraphics g, ItemStack stack, int x, int y)
    {
        g.renderFakeItem(stack, x, y);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F); // above the item, which renders at a raised depth
        g.fill(x, y, x + 16, y + 16, 0xA08B8B8B);
        g.pose().popPose();
    }

    /** Vanilla-style 18x18 inset slot. */
    private static void drawSlot(GuiGraphics g, int x, int y)
    {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }
}
