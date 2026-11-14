package com.thevoos.megatools.client;

import com.thevoos.megatools.GuideCompat;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.menu.GuiLayout;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.network.OpenSwapRulesPayload;
import com.thevoos.megatools.network.RenameToolPayload;
import com.thevoos.megatools.network.SwapToolPayload;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
 *    Row 1 = paxel abilities, row 2 = Support Tool abilities (only shown while a Support Tool is in
 *    the slot beside them), last row = Delayed Break (Mega Tool only) + Place Torch.
 *  - Left of the buttons: Support Tool slot (row 2) and torch slot (last row). Right column: plate
 *    slots (the active plate is outlined in gold). Empty special slots show a faded placeholder icon.
 *  - Wrench storage at the bottom of the tool section, then the player inventory. The "Custom Rule"
 *    button on its label row opens the Swap Rules GUI (SwapRulesScreen).
 *    Hover a stored tool and press R to close the GUI and swap straight to it.
 *
 * Everything is drawn with fills (no texture file needed). The title bar colour comes from
 * ToolTier#accentColor, and the layout height depends on the tier, so every tool's GUI is unique.
 */
public class ToolScreen extends AbstractContainerScreen<ToolMenu>
{
    // Index i = button id = ToolSettings flag (1 << i)
    private static final String[] TOGGLE_KEYS = {"strip", "path", "till", "shear", "brush", "ignite", "delayed_break", "place_torch", "light"};
    /** Button id of the small light on/off button under the torch slot (flag LIGHT_OFF, stored inverted). */
    private static final int LIGHT_BUTTON = 8;
    private static final int LIGHT_BUTTON_W = 18;
    private static final int LIGHT_BUTTON_H = 10;
    /** Fits between the last button row and the storage slots. */
    private static final int SWAP_RULES_BUTTON_H = 10;
    private static final String ON_SYMBOL = "\u2714 ";  // heavy check mark
    private static final String OFF_SYMBOL = "\u2716 "; // heavy multiplication x
    private static final int TOGGLE_H = 16;
    private static final int LABEL_COLOR = 0x404040;
    private static final int HINT_COLOR = 0x707070;
    /** Shown in the Shear / Brush / Ignite row while the Support Tool slot is empty. */
    private static final Component SUPPORT_HINT = Component.translatable("gui.megatools.support_hint");

    // Created per screen (not static) so they are never built before items are registered.
    private final ItemStack lightPlaceholder = new ItemStack(Items.TORCH);
    private final ItemStack platePlaceholder = new ItemStack(ModItems.ENCHANTMENT_PLATE.get());
    private final ItemStack supportPlaceholder = new ItemStack(ModItems.SUPPORT_TOOL.get());

    private final Button[] toggleButtons = new Button[TOGGLE_KEYS.length];
    private EditBox nameField;
    private String lastSentName = "";
    private int lastFlags = -1;
    private boolean lastSupport;
    /** Pixels the "Tool Storage" label may use before it would reach the Custom Rule button. */
    private int storageLabelRoom = Integer.MAX_VALUE;

    public ToolScreen(ToolMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = menu.getLayout().width();
        this.imageHeight = menu.getLayout().height();
    }

    /**
     * Client layout factory (registered in MegaToolsClient): measures this tier's toggle labels with the
     * current font, so every button is wide enough for its whole text (both the on and off version).
     */
    public static GuiLayout createLayout(ToolTier tier)
    {
        Font font = Minecraft.getInstance().font;
        int gridW = 0;
        int lastRowW = 0;
        for (int i = 0; i < TOGGLE_KEYS.length; i++)
        {
            if (!tier.hasToggle(i) || i == LIGHT_BUTTON)
                continue;
            int w = Math.max(font.width(toggleLabel(i, true)), font.width(toggleLabel(i, false)));
            if (i < 6)
                gridW = Math.max(gridW, w);
            else
                lastRowW = Math.max(lastRowW, w);
        }
        int hintW = tier.supportTools() ? font.width(SUPPORT_HINT) : 0;
        return GuiLayout.of(tier, gridW, lastRowW, hintW);
    }

    /** "[check] Strip" in green when enabled, "[cross] Strip" in red when disabled. */
    private static Component toggleLabel(int buttonId, boolean enabled)
    {
        return Component.literal(enabled ? ON_SYMBOL : OFF_SYMBOL)
                .append(Component.translatable("gui.megatools.toggle." + TOGGLE_KEYS[buttonId]))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    // =====================================================================================
    //  Widgets
    // =====================================================================================

    @Override
    protected void init()
    {
        super.init(); // computes leftPos / topPos

        // ---- Optional "?" button: opens the in-game guide when GuideME is installed ----
        boolean guide = GuideCompat.isLoaded();
        if (guide)
            addRenderableWidget(Button.builder(Component.literal("?"),
                            b -> GuideCompat.open(minecraft.player, "tool_menu.md"))
                    .bounds(leftPos + imageWidth - 18, topPos + 4, 12, 12)
                    .tooltip(Tooltip.create(Component.translatable("gui.megatools.open_guide")))
                    .build());

        // ---- Renamable title ----
        String currentName = menu.getTool().getHoverName().getString();
        nameField = new EditBox(this.font, leftPos + 10, topPos + 6, imageWidth - 20 - (guide ? 12 : 0), 10,
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
        // Widths come from the layout, which measured the labels (see createLayout).
        ToolTier tier = menu.getTier();
        GuiLayout layout = menu.getLayout();
        int lastRowY = topPos + GuiLayout.TOP + (tier.toggleRows() - 1) * GuiLayout.ROW;
        int lastRowIndex = 0;
        for (int i = 0; i < TOGGLE_KEYS.length; i++)
        {
            if (!tier.hasToggle(i) || i == LIGHT_BUTTON)
                continue;
            final int buttonId = i;
            int x, y, width;
            if (i < 6)
            {
                x = leftPos + layout.toggleX() + (i % 3) * (layout.toggleW() + GuiLayout.TOGGLE_GAP);
                y = topPos + GuiLayout.TOP + (i / 3) * GuiLayout.ROW;
                width = layout.toggleW();
            }
            else
            {
                x = leftPos + layout.toggleX() + lastRowIndex++ * (layout.lastRowToggleW() + GuiLayout.TOGGLE_GAP);
                y = lastRowY;
                width = layout.lastRowToggleW();
            }
            toggleButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> onToggle(buttonId))
                    .bounds(x, y, width, TOGGLE_H)
                    .build());
        }

        // ---- Light on/off: small button just under the torch slot (outside the button grid) ----
        if (tier.hasToggle(LIGHT_BUTTON))
            toggleButtons[LIGHT_BUTTON] = addRenderableWidget(Button.builder(Component.empty(), b -> onToggle(LIGHT_BUTTON))
                    .bounds(leftPos + layout.lightX() - 1, topPos + layout.lightY() + 18, LIGHT_BUTTON_W, LIGHT_BUTTON_H)
                    .build());

        // ---- Custom Rule (opens the Swap Rules GUI): small button on the "Tool Storage" label row, ending at the storage's right edge ----
        if (tier.hasAutoSwap())
        {
            Component label = Component.translatable("gui.megatools.swap_rules.button");
            int width = font.width(label) + 8;
            storageLabelRoom = layout.storageX() + GuiLayout.STORAGE_COLUMNS * 18 - 1 - width - 3 - layout.storageLabelX();
            addRenderableWidget(Button.builder(label, b -> openSwapRules())
                    .bounds(leftPos + layout.storageX() + GuiLayout.STORAGE_COLUMNS * 18 - 1 - width,
                            topPos + layout.storageLabelY() - 1, width, SWAP_RULES_BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable("gui.megatools.swap_rules.open")))
                    .build());
        }
        lastFlags = -1;
        refreshToggles();
    }

    /** The server closes this menu and opens the Swap Rules GUI of the same tool (see SwapRulesScreen). */
    private void openSwapRules()
    {
        commitRename(); // before the menu changes, so the new name isn't lost
        PacketDistributor.sendToServer(OpenSwapRulesPayload.INSTANCE);
    }

    /** Resizing the window rebuilds the widgets: keep a name that is typed but not confirmed yet. */
    @Override
    public void resize(Minecraft minecraft, int width, int height)
    {
        String typed = nameField.getValue();
        boolean focused = nameField.isFocused();
        String sent = lastSentName;
        super.resize(minecraft, width, height); // runs init() again
        nameField.setValue(typed);
        lastSentName = sent;
        if (focused)
            setFocused(nameField);
    }

    /** Button ids 3-5 (Shear, Brush, Ignite) need a Support Tool. */
    private static boolean isSupportToggle(int buttonId)
    {
        return buttonId >= 3 && buttonId <= 5;
    }

    /** Sends a vanilla "menu button" packet; the server flips the flag in ToolMenu#clickMenuButton. */
    private void onToggle(int buttonId)
    {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    /**
     * Re-labels the buttons when the synced settings change (symbol + colour + tooltip), and shows the
     * Shear / Brush / Ignite buttons only while a Support Tool is in its slot.
     */
    private void refreshToggles()
    {
        ItemStack tool = menu.getTool();
        int flags = ToolData.getSettings(tool).flags();
        boolean support = ToolData.hasSupportTool(tool);
        if (flags == lastFlags && support == lastSupport)
            return;
        lastFlags = flags;
        lastSupport = support;

        for (int i = 0; i < toggleButtons.length; i++)
        {
            Button button = toggleButtons[i];
            if (button == null)
                continue;
            boolean set = (flags & (1 << i)) != 0; // button i <-> ToolSettings flag (1 << i)
            boolean enabled = i == LIGHT_BUTTON ? !set : set; // the light flag is stored as LIGHT_OFF
            String key = "gui.megatools.toggle." + TOGGLE_KEYS[i];
            button.visible = !isSupportToggle(i) || support;
            button.setMessage(i == LIGHT_BUTTON
                    ? Component.literal(enabled ? ON_SYMBOL.trim() : OFF_SYMBOL.trim()).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
                    : toggleLabel(i, enabled));
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
        // R on a stored tool never gets here: KeyboardHandlerMixin handles it before any other mod (tryQuickSwap).
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
     * Quick swap from the GUI, called by KeyboardHandlerMixin for every key press while this screen is open,
     * before the screen itself and before other mods. The screen is closed BEFORE the swap packet is sent: packets arrive in
     * order, so the server closes the menu first and the stored tool can't also be taken out of
     * the still-open storage slot. (Closing also sends any pending rename, so the swapped-out
     * Mega Tools tool keeps its new name.)
     *
     * Returns true only if the key was the swap key AND a stored tool is hovered, so the key is
     * consumed in that case only and other mods (e.g. EMI's / JEI's R = show recipe) work everywhere else.
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
        else if (hoveredSlot instanceof ToolMenu.SupportSlot supportSlot && !supportSlot.hasItem())
        {
            graphics.renderTooltip(font, Component.translatable("gui.megatools.support_slot"), mouseX, mouseY);
        }
        else if (hoveredSlot instanceof ToolMenu.PlateSlot plateSlot && !plateSlot.hasItem())
        {
            boolean primary = plateSlot.getContainerSlot() == ToolData.PRIMARY;
            graphics.renderTooltip(font, primary ? Component.translatable("gui.megatools.plate.primary")
                    : Component.translatable("gui.megatools.plate.secondary", ModKeyMappings.SWITCH_PLATE.getTranslatedKeyMessage()),
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
                else if (slot instanceof ToolMenu.SupportSlot)
                    drawGhostItem(graphics, supportPlaceholder, x + slot.x, y + slot.y);
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
        // Empty Support Tool slot: a hint where the Shear / Brush / Ignite buttons would be.
        if (menu.getTier().supportTools() && !lastSupport)
        {
            int rowY = GuiLayout.TOP + GuiLayout.SUPPORT_ROW * GuiLayout.ROW;
            graphics.drawString(font, SUPPORT_HINT, layout.toggleX() + (layout.gridW() - font.width(SUPPORT_HINT)) / 2,
                    rowY + 4, HINT_COLOR, false);
        }
        if (layout.storageRows() > 0)
        {
            // Long translations are cut short rather than drawn under the Custom Rule button.
            String label = Component.translatable("gui.megatools.tool_storage").getString();
            if (font.width(label) > storageLabelRoom)
                label = font.plainSubstrByWidth(label, Math.max(0, storageLabelRoom - font.width("..."))) + "...";
            graphics.drawString(font, label, layout.storageLabelX(), layout.storageLabelY(), LABEL_COLOR, false);
        }
        graphics.drawString(font, playerInventoryTitle, layout.invLabelX(), layout.invLabelY(), LABEL_COLOR, false);
    }

    /** Vanilla-style bevelled grey panel. */
    static void drawPanel(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);          // top highlight
        g.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);          // left highlight
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);  // bottom shadow
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF555555);  // right shadow
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
    }

    /** Draws an item, then a translucent slot-coloured layer over it so it reads as a placeholder. */
    static void drawGhostItem(GuiGraphics g, ItemStack stack, int x, int y)
    {
        g.renderFakeItem(stack, x, y);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F); // above the item, which renders at a raised depth
        g.fill(x, y, x + 16, y + 16, 0xA08B8B8B);
        g.pose().popPose();
    }

    /** Vanilla-style 18x18 inset slot. */
    static void drawSlot(GuiGraphics g, int x, int y)
    {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }
}
