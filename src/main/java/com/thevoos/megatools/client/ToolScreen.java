package com.thevoos.megatools.client;

import com.thevoos.megatools.Config;
import com.thevoos.megatools.GuideCompat;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.menu.GuiLayout;
import com.thevoos.megatools.menu.MenuStyle;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.network.OpenSwapRulesPayload;
import com.thevoos.megatools.network.RenameToolPayload;
import com.thevoos.megatools.network.SwapToolPayload;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
 *    Row 1 = Break + Attack (what left click does), row 2 = paxel abilities, row 3 = Support Tool
 *    abilities (only shown while a Support Tool is in the slot beside them), last row = Delayed Break
 *    (Mega Tool only) + Place Torch.
 *  - Left of the buttons: Support Tool slot (row 2) and torch slot (last row). Right column: plate
 *    slots (the active plate is outlined in gold). Empty special slots show a faded placeholder icon.
 *  - Wrench storage at the bottom of the tool section, then the player inventory. The "Custom Rule"
 *    button on its label row opens the Swap Rules GUI (SwapRulesScreen).
 *    Hover a stored tool and press R to close the GUI and swap straight to it.
 *
 * ICON MENU (MenuStyle.ICONS, the player's choice): the same screen with GuiLayout#ofIcons. Every toggle
 * is an IconToggleButton (an item icon; green frame = on, red frame + dimmed + struck through = off) with
 * its name in the tooltip, and all slots sit in a 2x2 block at the right. Only the positions and the look
 * of the toggles differ: slots, button ids and everything sent to the server are the same.
 *
 * The panel and the slots are cut out of vanilla's chest GUI texture (see drawPanel), so resource packs
 * that restyle the GUIs (dark themes...) restyle this one too; no texture file of our own. The title
 * bar colour comes from ToolTier#accentColor, and the layout height depends on the tier, so every tool's GUI is unique.
 */
public class ToolScreen extends AbstractContainerScreen<ToolMenu>
{
    // Index i = button id = ToolSettings flag (1 << i)
    private static final String[] TOGGLE_KEYS = {"strip", "path", "till", "shear", "brush", "ignite", "delayed_break", "place_torch", "light", "break", "attack"};
    /** Button id of the small light on/off button under the torch slot (flag LIGHT_OFF, stored inverted). */
    private static final int LIGHT_BUTTON = 8;
    /** Button ids of the first row (flags BREAK_OFF / ATTACK_OFF, stored inverted). */
    private static final int BREAK_BUTTON = 9;
    private static final int ATTACK_BUTTON = 10;
    private static final int LIGHT_BUTTON_W = 18;
    private static final int LIGHT_BUTTON_H = 10;
    /** Fits between the last button row and the storage slots. */
    private static final int SWAP_RULES_BUTTON_H = 10;
    private static final String ON_SYMBOL = "\u2714 ";  // heavy check mark
    private static final String OFF_SYMBOL = "\u2716 "; // heavy multiplication x
    private static final int TOGGLE_H = 16;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int HINT_COLOR = 0xFF707070;
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
        super(menu, playerInventory, title, menu.getLayout().width(), menu.getLayout().height());
    }

    /**
     * Client layout factory (registered in MegaToolsClient): measures this tier's toggle labels with the
     * current font, so every button is wide enough for its whole text (both the on and off version).
     */
    public static GuiLayout createLayout(ToolTier tier)
    {
        // The player's choice of menu (asked the first time, see MenuStyleScreen). Not chosen yet: classic.
        if (Config.MENU_STYLE.get() == MenuStyle.ICONS)
            return GuiLayout.ofIcons(tier);

        Font font = Minecraft.getInstance().font;
        int gridW = 0;
        int lastRowW = 0;
        int topRowW = 0;
        for (int i = 0; i < TOGGLE_KEYS.length; i++)
        {
            if (!tier.hasToggle(i) || i == LIGHT_BUTTON)
                continue;
            int w = Math.max(font.width(toggleLabel(i, true)), font.width(toggleLabel(i, false)));
            if (i < 6)
                gridW = Math.max(gridW, w);
            else if (isTopRowToggle(i))
                topRowW = Math.max(topRowW, w);
            else
                lastRowW = Math.max(lastRowW, w);
        }
        int hintW = tier.supportTools() ? font.width(SUPPORT_HINT) : 0;
        return GuiLayout.of(tier, gridW, lastRowW, hintW, topRowW);
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
        nameField.setTextColor(0xFFFFFFFF);
        nameField.setValue(currentName);
        lastSentName = currentName;
        addRenderableWidget(nameField);

        // ---- Ability toggles ----
        // First row: Break and Attack (two half-width buttons). Ability toggles 0-5 fill a 3-wide grid
        // under it. The last row holds Place Torch (all tools), shared with Delayed Break on the Mega
        // Tool (two half-width buttons).
        // Widths come from the layout, which measured the labels (see createLayout).
        ToolTier tier = menu.getTier();
        GuiLayout layout = menu.getLayout();
        if (layout.icons())
            addIconToggles(tier);
        int lastRowY = topPos + GuiLayout.TOP + (tier.toggleRows() - 1) * GuiLayout.ROW;
        int lastRowIndex = 0;
        for (int i = 0; i < TOGGLE_KEYS.length && !layout.icons(); i++)
        {
            if (!tier.hasToggle(i) || i == LIGHT_BUTTON)
                continue;
            final int buttonId = i;
            int x, y, width;
            if (i < 6)
            {
                x = leftPos + layout.toggleX() + (i % 3) * (layout.toggleW() + GuiLayout.TOGGLE_GAP);
                y = topPos + GuiLayout.TOP + (GuiLayout.GRID_ROW + i / 3) * GuiLayout.ROW;
                width = layout.toggleW();
            }
            else if (isTopRowToggle(i))
            {
                x = leftPos + layout.toggleX() + (i - BREAK_BUTTON) * (layout.halfToggleW() + GuiLayout.TOGGLE_GAP);
                y = topPos + GuiLayout.TOP;
                width = layout.halfToggleW();
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

    /** Icon menu, row 1: what the tool itself does. Delayed Break is skipped on the paxels, the rest moves up. */
    private static final int[] ICON_ROW_0_BUTTONS = {BREAK_BUTTON, ATTACK_BUTTON, 6, 0, 1, 2};
    /** Icon menu, row 2: Place Torch, then the three Support Tool abilities. */
    private static final int[] ICON_ROW_1_BUTTONS = {7, 3, 4, 5};

    /** The icon menu's toggles: one 18x18 item icon each, side by side like slots (see GuiLayout#ofIcons). */
    private void addIconToggles(ToolTier tier)
    {
        int[][] rows = {ICON_ROW_0_BUTTONS, ICON_ROW_1_BUTTONS};
        for (int row = 0; row < rows.length; row++)
        {
            int column = 0;
            for (int buttonId : rows[row])
            {
                if (!tier.hasToggle(buttonId))
                    continue;
                int x = leftPos + GuiLayout.ICON_X + column++ * 18;
                int y = topPos + (row == 0 ? GuiLayout.ICON_ROW_0 : GuiLayout.ICON_ROW_1);
                toggleButtons[buttonId] = addRenderableWidget(new IconToggleButton(x, y, toggleIcon(buttonId, tier),
                        Component.translatable("gui.megatools.toggle." + TOGGLE_KEYS[buttonId]), b -> onToggle(buttonId)));
            }
        }
    }

    /** The item that stands for a toggle in the icon menu; the paxel abilities show the tool of the menu's own tier. */
    private static ItemStack toggleIcon(int buttonId, ToolTier tier)
    {
        return new ItemStack(switch (buttonId)
        {
            case BREAK_BUTTON -> switch (tier) { case STONE -> Items.STONE_PICKAXE; case IRON -> Items.IRON_PICKAXE; case DIAMOND -> Items.DIAMOND_PICKAXE; case MEGA -> Items.NETHERITE_PICKAXE; };
            case ATTACK_BUTTON -> switch (tier) { case STONE -> Items.STONE_SWORD; case IRON -> Items.IRON_SWORD; case DIAMOND -> Items.DIAMOND_SWORD; case MEGA -> Items.NETHERITE_SWORD; };
            case 0 -> switch (tier) { case STONE -> Items.STONE_AXE; case IRON -> Items.IRON_AXE; case DIAMOND -> Items.DIAMOND_AXE; case MEGA -> Items.NETHERITE_AXE; };
            case 1 -> switch (tier) { case STONE -> Items.STONE_SHOVEL; case IRON -> Items.IRON_SHOVEL; case DIAMOND -> Items.DIAMOND_SHOVEL; case MEGA -> Items.NETHERITE_SHOVEL; };
            case 2 -> switch (tier) { case STONE -> Items.STONE_HOE; case IRON -> Items.IRON_HOE; case DIAMOND -> Items.DIAMOND_HOE; case MEGA -> Items.NETHERITE_HOE; };
            case 3 -> Items.SHEARS;
            case 4 -> Items.BRUSH;
            case 5 -> Items.FLINT_AND_STEEL;
            case 6 -> Items.CLOCK;
            default -> Items.TORCH; // 7: Place Torch
        });
    }

    /** The server closes this menu and opens the Swap Rules GUI of the same tool (see SwapRulesScreen). */
    private void openSwapRules()
    {
        commitRename(); // before the menu changes, so the new name isn't lost
        ClientPacketDistributor.sendToServer(OpenSwapRulesPayload.INSTANCE);
    }

    /** Resizing the window rebuilds the widgets: keep a name that is typed but not confirmed yet. */
    @Override
    public void resize(int width, int height)
    {
        String typed = nameField.getValue();
        boolean focused = nameField.isFocused();
        String sent = lastSentName;
        super.resize(width, height); // runs init() again
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

    /** Button ids 9-10 (Break, Attack) share the first row. */
    private static boolean isTopRowToggle(int buttonId)
    {
        return buttonId == BREAK_BUTTON || buttonId == ATTACK_BUTTON;
    }

    /** Light, Break and Attack are stored inverted (LIGHT_OFF, BREAK_OFF, ATTACK_OFF). */
    private static boolean isInvertedToggle(int buttonId)
    {
        return buttonId == LIGHT_BUTTON || isTopRowToggle(buttonId);
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
            boolean enabled = isInvertedToggle(i) != set; // e.g. the light flag is stored as LIGHT_OFF
            String key = "gui.megatools.toggle." + TOGGLE_KEYS[i];
            Component description = Component.translatable(key + ".desc",
                    Component.translatable(enabled ? "gui.megatools.enabled" : "gui.megatools.disabled"));
            if (button instanceof IconToggleButton icon)
            {
                // No words on the button, so its name leads the tooltip. Without a Support Tool the three
                // buttons it unlocks stay in place, greyed out, and say what is missing.
                boolean unlocked = !isSupportToggle(i) || support;
                icon.setOn(enabled);
                icon.active = unlocked;
                Component name = Component.translatable(key).withStyle(ChatFormatting.BOLD);
                icon.setTooltip(Tooltip.create(Component.empty().append(name).append("\n")
                        .append(unlocked ? description : SUPPORT_HINT)));
                continue;
            }
            button.visible = !isSupportToggle(i) || support;
            button.setMessage(i == LIGHT_BUTTON
                    ? Component.literal(enabled ? ON_SYMBOL.trim() : OFF_SYMBOL.trim()).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
                    : toggleLabel(i, enabled));
            button.setTooltip(Tooltip.create(description));
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
            ClientPacketDistributor.sendToServer(new RenameToolPayload(value));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        // R on a stored tool never gets here: KeyboardHandlerMixin handles it before any other mod (tryQuickSwap).
        int keyCode = event.key();
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
                return nameField.keyPressed(event) || nameField.canConsumeInput();
        }
        return super.keyPressed(event);
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
    public boolean tryQuickSwap(KeyEvent event)
    {
        if (nameField.isFocused())
            return false; // typing "r" in the name field
        if (!ModKeyMappings.SWAP_TOOL.matches(event) || !ModKeyMappings.modifierMatches(ModKeyMappings.SWAP_TOOL))
            return false;
        if (!(hoveredSlot instanceof ToolMenu.StorageSlot storageSlot) || !storageSlot.hasItem())
            return false;

        int storageIndex = storageSlot.getContainerSlot();
        onClose();
        ClientPacketDistributor.sendToServer(new SwapToolPayload(storageIndex));
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        // Clicking anywhere else confirms the rename.
        if (nameField.isFocused() && !nameField.isMouseOver(event.x(), event.y()))
        {
            commitRename();
            nameField.setFocused(false);
        }
        return super.mouseClicked(event, doubleClick);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick); // also queues the item tooltips

        // Hint tooltips for empty plate slots and the title.
        if (hoveredSlot instanceof ToolMenu.LightSlot lightSlot && !lightSlot.hasItem())
        {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.megatools.light_slot"), mouseX, mouseY);
        }
        else if (hoveredSlot instanceof ToolMenu.SupportSlot supportSlot && !supportSlot.hasItem())
        {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.megatools.support_slot"), mouseX, mouseY);
        }
        else if (hoveredSlot instanceof ToolMenu.PlateSlot plateSlot && !plateSlot.hasItem())
        {
            boolean primary = plateSlot.getContainerSlot() == ToolData.PRIMARY;
            graphics.setTooltipForNextFrame(font, primary ? Component.translatable("gui.megatools.plate.primary")
                    : Component.translatable("gui.megatools.plate.secondary", ModKeyMappings.SWITCH_PLATE.getTranslatedKeyMessage()),
                    mouseX, mouseY);
        }
        else if (!nameField.isFocused() && nameField.isMouseOver(mouseX, mouseY))
        {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.megatools.rename_hint"), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTick); // darkened world behind the menu
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
            int plateX = active == ToolData.PRIMARY ? layout.plate0X() : layout.plate1X();
            int plateY = active == ToolData.PRIMARY ? layout.plate0Y() : layout.plate1Y();
            graphics.outline(x + plateX - 2, y + plateY - 2, 20, 20, 0xFFE8B923);
        }
    }

    /** Section labels. The title is the EditBox, so the default title/inventory labels are replaced. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        GuiLayout layout = menu.getLayout();
        // Empty Support Tool slot: a hint where the Shear / Brush / Ignite buttons would be.
        if (menu.getTier().supportTools() && !lastSupport && !layout.icons())
        {
            int rowY = GuiLayout.TOP + GuiLayout.SUPPORT_ROW * GuiLayout.ROW;
            graphics.text(font, SUPPORT_HINT, layout.toggleX() + (layout.gridW() - font.width(SUPPORT_HINT)) / 2,
                    rowY + 4, HINT_COLOR, false);
        }
        if (layout.storageRows() > 0)
        {
            // Long translations are cut short rather than drawn under the Custom Rule button.
            String label = Component.translatable("gui.megatools.tool_storage").getString();
            if (font.width(label) > storageLabelRoom)
                label = font.plainSubstrByWidth(label, Math.max(0, storageLabelRoom - font.width("..."))) + "...";
            graphics.text(font, label, layout.storageLabelX(), layout.storageLabelY(), LABEL_COLOR, false);
        }
        graphics.text(font, playerInventoryTitle, layout.invLabelX(), layout.invLabelY(), LABEL_COLOR, false);
    }

    /**
     * Vanilla's chest GUI (176 x 222 on a 256 x 256 sheet). The panel and slots are assembled from its
     * pieces, so whatever a resource pack does to the chest GUI (dark themes...) applies here as well.
     */
    private static final Identifier CONTAINER_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEX_W = 176;
    private static final int TEX_H = 222;
    /** Border thickness, including the rounded corners. */
    private static final int EDGE = 7;
    /** Plain background without slots: the title strip, right under the top border. */
    private static final int PLAIN_U = EDGE;
    private static final int PLAIN_V = EDGE;
    private static final int PLAIN_W = TEX_W - 2 * EDGE;
    private static final int PLAIN_H = 10;
    /** First slot of the chest GUI (18 x 18 with its frame). */
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 17;

    /** A panel of any size in the style of the vanilla container GUIs: corners, tiled edges, tiled plain middle. */
    static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h)
    {
        int right = x + w - EDGE;
        int bottom = y + h - EDGE;
        // Middle and the four edges, tiled from the plain strip and the border beside it
        for (int ty = y + EDGE; ty < bottom; ty += PLAIN_H)
        {
            int th = Math.min(PLAIN_H, bottom - ty);
            for (int tx = x + EDGE; tx < right; tx += PLAIN_W)
                g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, tx, ty, PLAIN_U, PLAIN_V, Math.min(PLAIN_W, right - tx), th, 256, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, ty, 0, PLAIN_V, EDGE, th, 256, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, right, ty, TEX_W - EDGE, PLAIN_V, EDGE, th, 256, 256);
        }
        for (int tx = x + EDGE; tx < right; tx += PLAIN_W)
        {
            int tw = Math.min(PLAIN_W, right - tx);
            g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, tx, y, PLAIN_U, 0, tw, EDGE, 256, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, tx, bottom, PLAIN_U, TEX_H - EDGE, tw, EDGE, 256, 256);
        }
        // Corners
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, y, 0, 0, EDGE, EDGE, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, right, y, TEX_W - EDGE, 0, EDGE, EDGE, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, bottom, 0, TEX_H - EDGE, EDGE, EDGE, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, right, bottom, TEX_W - EDGE, TEX_H - EDGE, EDGE, EDGE, 256, 256);
    }

    /** Draws an item, then the slot's own background half-transparent over it, so it reads as a placeholder. */
    static void drawGhostItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y)
    {
        g.fakeItem(stack, x, y);
        // Drawn after the item, so it lands above it (the GUI puts later overlapping elements on top).
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, y, SLOT_U + 1, SLOT_V + 1, 16, 16, 256, 256,
                ARGB.white(0.63F));
    }

    /** An 18x18 slot, the same one the vanilla chest GUI shows. */
    static void drawSlot(GuiGraphicsExtractor g, int x, int y)
    {
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, y, SLOT_U, SLOT_V, 18, 18, 256, 256);
    }
}
