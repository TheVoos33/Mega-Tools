package com.thevoos.megatools.client;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.menu.SwapRulesMenu;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.network.ApplyDefaultRulesPayload;
import com.thevoos.megatools.network.OpenToolGuiPayload;
import com.thevoos.megatools.network.SetSwapRulesPayload;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.thevoos.megatools.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import com.thevoos.megatools.network.ModPackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Swap Rules GUI: opened with the "Custom Rule" button of the tool GUI (ToolScreen), "<" goes back.
 *
 *  - Stored tools: one cell per kind of tool in the tool's storage. Click one to select it (gold
 *    outline). The small number is how many filters it has. A tool that has filters but isn't in
 *    storage right now is shown faded, so its filters can still be seen and removed.
 *  - Filter slots (2 x 9) of the selected tool. A filter is a block; while the player looks at that
 *    block with this tool in hand, the selected stored tool is pre-selected (AutoSwapClient).
 *      set    : click a slot while carrying an item from the inventory, Shift-click an item in the
 *               inventory, or drag an item from JEI / EMI onto a slot (compat package). Nothing is
 *               used up: the slot only remembers the item.
 *      click  : next mode: "only this block" -> "every block with one of its tags" (purple frame +
 *               "#") -> "every block from its mod" (blue frame + "@"). The signs are the ones JEI and
 *               EMI use for tag and mod search, so colour is not the only cue.
 *      scroll : on a tag filter, pick another of the block's tags (the tooltip lists them).
 *      right-click : remove.
 *    Default rules (config/megatools-default-rules.json, see DefaultSwapRules) are copied onto the tool
 *    and show up here as ordinary filters: they can be changed and removed like any other.
 *  - Copy / Paste (title bar): Copy remembers ALL of this tool's rules, Paste replaces the rules of
 *    the tool whose GUI is open with them (and passes on which default rules the copied tool already
 *    received, so a default removed there doesn't come back here). That is how rules get from one tool to the next: the
 *    copy is kept until the game is closed.
 *
 * The rules live on the tool stack (SwapRules component). Every change is sent to the server as the
 * complete new rule list (SetSwapRulesPayload); the screen shows its own copy at once and takes over
 * whatever the server syncs back.
 */
public class SwapRulesScreen extends AbstractContainerScreen<SwapRulesMenu>
{
    private static final int LABEL_COLOR = 0x404040;
    private static final int HINT_COLOR = 0x707070;
    private static final int GOLD = 0xFFE8B923;
    private static final int MOD_BLUE = 0xFF2F7BE0;
    private static final int TAG_PURPLE = 0xFFA24BD6;
    /** Tags listed in a tag filter's tooltip at once (a window around the chosen one). */
    private static final int TAG_LINES = 9;
    private static final int HOVER = 0x80FFFFFF;
    private static final int DROP_TARGET = 0x8822BB33;
    private static final int STATUS_TICKS = 40;

    /** Copied rules. Static: it has to outlive this screen to reach another tool's GUI. */
    private static Clipboard clipboard;

    /** @param appliedDefaults the default rules the copied tool had already received */
    private record Clipboard(SwapRules rules, SwapRules appliedDefaults) {}

    /** One cell of the stored-tool selector. */
    private record Entry(ResourceLocation id, ItemStack icon, boolean stored) {}

    private final List<Entry> entries = new ArrayList<>();
    private final Map<ResourceLocation, ItemStack> icons = new HashMap<>();
    private final Map<ResourceLocation, List<ResourceLocation>> blockTags = new HashMap<>();
    private ResourceLocation selectedTool;
    /** What the screen shows: the synced rules plus any change the server hasn't confirmed yet. */
    private SwapRules rules = SwapRules.EMPTY;
    private SwapRules lastSynced;
    // ---- Per-frame lookups, rebuilt only when the rules or the selection change ----
    private SwapRules cachedRules;
    private ResourceLocation cachedTool;
    private List<SwapRules.Rule> selectedRules = List.of();
    /** Filters per stored tool (the small number in its cell). */
    private final Map<ResourceLocation, Integer> filterCounts = new HashMap<>();
    private Button pasteButton;
    private Component status;
    private int statusTicks;

    public SwapRulesScreen(SwapRulesMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = SwapRulesMenu.WIDTH;
        this.imageHeight = menu.height();
    }

    // =====================================================================================
    //  Widgets
    // =====================================================================================

    @Override
    protected void init()
    {
        super.init(); // computes leftPos / topPos

        addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> ModPackets.sendToServer(OpenToolGuiPayload.INSTANCE)) // the server reopens the tool GUI
                .bounds(leftPos + 7, topPos + 4, 12, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.megatools.swap_rules.back")))
                .build());

        Component paste = Component.translatable("gui.megatools.swap_rules.paste");
        int pasteW = font.width(paste) + 8;
        int pasteX = leftPos + imageWidth - 7 - pasteW;
        pasteButton = addRenderableWidget(Button.builder(paste, b -> paste())
                .bounds(pasteX, topPos + 4, pasteW, 12)
                .build());

        Component copy = Component.translatable("gui.megatools.swap_rules.copy");
        int copyW = font.width(copy) + 8;
        addRenderableWidget(Button.builder(copy, b -> copy())
                .bounds(pasteX - 2 - copyW, topPos + 4, copyW, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.megatools.swap_rules.copy.desc")))
                .build());

        refreshPasteButton();
        refresh();
    }

    private void refreshPasteButton()
    {
        pasteButton.active = clipboard != null;
        pasteButton.setTooltip(Tooltip.create(Component.translatable(clipboard != null
                ? "gui.megatools.swap_rules.paste.desc" : "gui.megatools.swap_rules.paste.empty")));
    }

    private void copy()
    {
        clipboard = new Clipboard(rules, ToolData.getAppliedDefaults(menu.getTool()));
        refreshPasteButton();
        showStatus("gui.megatools.swap_rules.copied");
    }

    private void paste()
    {
        if (clipboard == null)
            return;
        apply(clipboard.rules());
        if (!clipboard.appliedDefaults().isEmpty())
            ModPackets.sendToServer(new ApplyDefaultRulesPayload(clipboard.appliedDefaults(), SwapRules.EMPTY));
        showStatus("gui.megatools.swap_rules.pasted");
    }

    /** Shown in the title bar for two seconds, in place of the title. */
    private void showStatus(String key)
    {
        status = Component.translatable(key);
        statusTicks = STATUS_TICKS;
    }

    // =====================================================================================
    //  Data
    // =====================================================================================

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (statusTicks > 0)
            statusTicks--;
        refresh();
    }

    /** Takes over newly synced rules and rebuilds the stored-tool cells (cheap: no item copies). */
    private void refresh()
    {
        ItemStack tool = menu.getTool();
        SwapRules synced = ToolData.getSwapRules(tool);
        if (!synced.equals(lastSynced))
        {
            lastSynced = synced;
            rules = synced;
        }

        // Stored tools first (storage order, each kind once), then tools that only have rules.
        entries.clear();
        Set<ResourceLocation> seen = new HashSet<>();
        ItemContainerContents storage = ModDataComponents.TOOL_STORAGE.getOrDefault(tool, ItemContainerContents.EMPTY);
        for (ItemStack stored : storage.nonEmptyItems())
        {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stored.getItem());
            if (seen.add(id))
                entries.add(new Entry(id, stored, true));
        }
        for (ResourceLocation id : rules.tools())
            if (seen.add(id))
                entries.add(new Entry(id, icon(id), false));
        while (entries.size() > menu.selectorCells())
            entries.remove(entries.size() - 1);

        if (selectedEntry() == null)
            selectedTool = entries.isEmpty() ? null : entries.get(0).id();
    }

    private Entry selectedEntry()
    {
        if (selectedTool != null)
            for (Entry entry : entries)
                if (entry.id().equals(selectedTool))
                    return entry;
        return null;
    }

    /** Stores the new rules locally and sends them to the server. */
    private void apply(SwapRules changed)
    {
        if (changed.equals(rules))
            return;
        rules = changed;
        ModPackets.sendToServer(new SetSwapRulesPayload(changed));
        playClick();
    }

    private void playClick()
    {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** Icon for an item id; a barrier if the item doesn't exist (its mod was removed). */
    private ItemStack icon(ResourceLocation id)
    {
        return icons.computeIfAbsent(id, key ->
        {
            Item item = BuiltInRegistries.ITEM.get(key); // minecraft:air if unknown
            return new ItemStack(item == Items.AIR ? Items.BARRIER : item);
        });
    }

    /** Block tags of the block this item places, sorted by name; empty if it places none. */
    private List<ResourceLocation> blockTags(ResourceLocation itemId)
    {
        return blockTags.computeIfAbsent(itemId, key ->
        {
            Block block = Block.byItem(BuiltInRegistries.ITEM.get(key));
            if (block == Blocks.AIR)
                return List.of();
            return block.builtInRegistryHolder().tags().map(TagKey::location).sorted().toList();
        });
    }

    /**
     * Drawing asks for the selected tool's filters and for every cell's filter count each frame. Both mean
     * walking the whole rule list, so they are kept until something changes (SwapRules objects are
     * immutable and replaced on every change, so comparing the references is enough).
     */
    private void updateCache()
    {
        boolean rulesChanged = rules != cachedRules;
        if (!rulesChanged && Objects.equals(selectedTool, cachedTool))
            return;
        if (rulesChanged)
            filterCounts.clear();
        cachedRules = rules;
        cachedTool = selectedTool;
        selectedRules = selectedTool == null ? List.of() : rules.forTool(selectedTool);
    }

    private int filterCount(ResourceLocation tool)
    {
        return filterCounts.computeIfAbsent(tool, id -> rules.forTool(id).size());
    }

    private static boolean exists(ResourceLocation id)
    {
        return BuiltInRegistries.ITEM.containsKey(id);
    }

    private Component itemName(ResourceLocation id)
    {
        return exists(id) ? icon(id).getHoverName() : Component.literal(id.toString());
    }

    private static String modName(String namespace)
    {
        return ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    // =====================================================================================
    //  Filter slots: used by this screen and by the JEI / EMI plugins (compat package)
    // =====================================================================================

    /** Screen areas of the filter slots, by slot index. Empty while no stored tool is selected. */
    public List<Rect2i> getFilterAreas()
    {
        List<Rect2i> areas = new ArrayList<>();
        if (selectedTool != null)
            for (int i = 0; i < SwapRulesMenu.FILTER_SLOTS; i++)
                areas.add(new Rect2i(filterX(i), filterY(i), 16, 16));
        return areas;
    }

    /** Puts the item into filter slot {@code index} of the selected tool (replace, or add if the slot is free). */
    public void setFilter(int index, ItemStack stack)
    {
        if (selectedTool != null && !stack.isEmpty())
            apply(rules.withTarget(selectedTool, index, BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    /** Adds the item as a new filter of the selected tool. */
    public void addFilter(ItemStack stack)
    {
        setFilter(SwapRulesMenu.FILTER_SLOTS, stack);
    }

    /** Light green over every filter slot: "you can drop it here" (used while EMI drags an item). */
    public void renderDropTargets(GuiGraphics graphics)
    {
        for (Rect2i area : getFilterAreas())
            graphics.fill(RenderType.guiOverlay(), area.getX(), area.getY(),
                    area.getX() + area.getWidth(), area.getY() + area.getHeight(), DROP_TARGET);
    }

    private int filterX(int index)
    {
        return leftPos + SwapRulesMenu.LEFT + (index % SwapRulesMenu.COLUMNS) * 18;
    }

    private int filterY(int index)
    {
        return topPos + menu.filterY() + (index / SwapRulesMenu.COLUMNS) * 18;
    }

    private int selectorX(int index)
    {
        return leftPos + SwapRulesMenu.LEFT + (index % SwapRulesMenu.COLUMNS) * 18;
    }

    private int selectorY(int index)
    {
        return topPos + SwapRulesMenu.SELECTOR_Y + (index / SwapRulesMenu.COLUMNS) * 18;
    }

    /** Index of the 18x18 cell under the mouse in a 9-wide grid starting at item position (0, gridY), or -1. */
    private int cellAt(double mouseX, double mouseY, int gridY, int cells)
    {
        double dx = mouseX - (leftPos + SwapRulesMenu.LEFT - 1);
        double dy = mouseY - (topPos + gridY - 1);
        if (dx < 0 || dy < 0 || dx >= SwapRulesMenu.COLUMNS * 18)
            return -1;
        int index = (int) (dy / 18) * SwapRulesMenu.COLUMNS + (int) (dx / 18);
        return index < cells ? index : -1;
    }

    private int selectorAt(double mouseX, double mouseY)
    {
        return cellAt(mouseX, mouseY, SwapRulesMenu.SELECTOR_Y, entries.size());
    }

    private int filterAt(double mouseX, double mouseY)
    {
        return cellAt(mouseX, mouseY, menu.filterY(), SwapRulesMenu.FILTER_SLOTS);
    }

    // =====================================================================================
    //  Input
    // =====================================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int entry = selectorAt(mouseX, mouseY);
        if (entry >= 0)
        {
            selectedTool = entries.get(entry).id();
            playClick();
            return true;
        }

        int filter = filterAt(mouseX, mouseY);
        if (filter >= 0)
        {
            if (selectedTool == null || (button != 0 && button != 1))
                return true;
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty())
                setFilter(filter, carried);                               // stays on the cursor: nothing is used up
            else if (filter < rules.forTool(selectedTool).size())
                apply(button == 0
                        ? rules.cycleMode(selectedTool, filter, blockTags(rules.forTool(selectedTool).get(filter).target()))
                        : rules.without(selectedTool, filter));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Mouse wheel over a tag filter: pick the previous / next tag of its block. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
    {
        int filter = filterAt(mouseX, mouseY);
        if (filter >= 0 && selectedTool != null && scrollY != 0)
        {
            List<SwapRules.Rule> mine = rules.forTool(selectedTool);
            if (filter < mine.size() && mine.get(filter).mode() == SwapRules.Mode.TAG)
            {
                SwapRules.Rule rule = mine.get(filter);
                List<ResourceLocation> tags = blockTags(rule.target());
                if (!tags.isEmpty())
                {
                    // Wheel up = previous, wheel down = next. An unknown tag (-1) starts at the first one.
                    int current = tags.indexOf(rule.tag().orElse(null));
                    int next = current < 0 ? 0 : Math.floorMod(current + (scrollY > 0 ? -1 : 1), tags.size());
                    apply(rules.withTag(selectedTool, filter, tags.get(next)));
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    /** Shift-click an item in the inventory: add it as a filter (there is nowhere to move it to anyway). */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
    {
        if (type == ClickType.QUICK_MOVE)
        {
            if (slot != null && slot.hasItem() && !(slot instanceof ToolMenu.LockedSlot))
                addFilter(slot.getItem());
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    // =====================================================================================
    //  Rendering
    // =====================================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY); // inventory item tooltips

        if (!menu.getCarried().isEmpty())
            return;
        updateCache();
        List<Component> lines = new ArrayList<>();
        int entry = selectorAt(mouseX, mouseY);
        int filter = filterAt(mouseX, mouseY);
        if (entry >= 0)
            entryTooltip(entries.get(entry), lines);
        else if (filter >= 0 && selectedTool != null)
            filterTooltip(filter, lines);
        if (!lines.isEmpty())
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void entryTooltip(Entry entry, List<Component> lines)
    {
        lines.add(entry.stored() ? entry.icon().getHoverName() : itemName(entry.id()));
        if (!entry.stored())
            lines.add(Component.translatable("gui.megatools.swap_rules.not_stored").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("gui.megatools.swap_rules.filter_count", rules.forTool(entry.id()).size())
                .withStyle(ChatFormatting.GRAY));
    }

    private void filterTooltip(int index, List<Component> lines)
    {
        List<SwapRules.Rule> mine = selectedRules;
        if (index >= mine.size())
        {
            lines.add(Component.translatable("gui.megatools.swap_rules.empty_slot"));
            lines.add(Component.translatable("gui.megatools.swap_rules.empty_slot.shift").withStyle(ChatFormatting.GRAY));
            return;
        }

        SwapRules.Rule rule = mine.get(index);
        List<ResourceLocation> tags = blockTags(rule.target());
        lines.add(itemName(rule.target()));
        switch (rule.mode())
        {
            case ITEM ->
            {
                lines.add(Component.translatable("gui.megatools.swap_rules.mode.item").withStyle(ChatFormatting.GREEN));
                // An item that places no block: matches while it is the part under the crosshair (pick block).
                if (Block.byItem(icon(rule.target()).getItem()) == Blocks.AIR)
                    lines.add(Component.translatable("gui.megatools.swap_rules.not_a_block").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.translatable(tags.isEmpty()
                        ? "gui.megatools.swap_rules.hint.to_mod" : "gui.megatools.swap_rules.hint.to_tag").withStyle(ChatFormatting.GRAY));
            }
            case TAG ->
            {
                ResourceLocation tag = rule.tag().orElseThrow();
                lines.add(Component.translatable("gui.megatools.swap_rules.mode.tag", "#" + tag).withStyle(ChatFormatting.LIGHT_PURPLE));
                tagList(tags, tags.indexOf(tag), lines);
                lines.add(Component.translatable("gui.megatools.swap_rules.hint.to_mod").withStyle(ChatFormatting.GRAY));
            }
            case MOD ->
            {
                lines.add(Component.translatable("gui.megatools.swap_rules.mode.mod", modName(rule.target().getNamespace()))
                        .withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("gui.megatools.swap_rules.hint.to_item").withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.translatable("gui.megatools.swap_rules.hint.remove").withStyle(ChatFormatting.GRAY));
    }

    /**
     * The block's tags with the chosen one marked ("> #tag", white; the others grey), so the player sees
     * what the mouse wheel will pick. Long lists show a window of TAG_LINES around the chosen tag.
     */
    private static void tagList(List<ResourceLocation> tags, int chosen, List<Component> lines)
    {
        if (tags.size() < 2)
            return;
        lines.add(Component.translatable("gui.megatools.swap_rules.hint.scroll", Math.max(chosen, 0) + 1, tags.size())
                .withStyle(ChatFormatting.GRAY));
        int from = Math.max(0, Math.min(chosen - TAG_LINES / 2, tags.size() - TAG_LINES));
        int to = Math.min(tags.size(), from + TAG_LINES);
        if (from > 0)
            lines.add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
        for (int i = from; i < to; i++)
            lines.add(Component.literal((i == chosen ? "> #" : "  #") + tags.get(i))
                    .withStyle(i == chosen ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        if (to < tags.size())
            lines.add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = leftPos;
        int y = topPos;
        updateCache();

        // Panel + tier-coloured title bar (same look as the tool GUI)
        ToolScreen.drawPanel(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x + 6, y + 4, x + imageWidth - 6, y + 16, menu.getTier().accentColor());

        // ---- Stored tools ----
        int hoveredEntry = selectorAt(mouseX, mouseY);
        for (int i = 0; i < menu.selectorCells(); i++)
        {
            int cx = selectorX(i);
            int cy = selectorY(i);
            ToolScreen.drawSlot(graphics, cx - 1, cy - 1);
            if (i >= entries.size())
                continue;
            Entry entry = entries.get(i);
            if (entry.stored())
                graphics.renderItem(entry.icon(), cx, cy);
            else
                ToolScreen.drawGhostItem(graphics, entry.icon(), cx, cy);
            int count = filterCount(entry.id());
            if (count > 0)
                graphics.renderItemDecorations(font, entry.icon(), cx, cy, String.valueOf(count));
            if (i == hoveredEntry)
                graphics.fill(RenderType.guiOverlay(), cx, cy, cx + 16, cy + 16, HOVER);
        }
        // Selection outline last, so neighbouring slot frames don't cover it.
        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).id().equals(selectedTool))
                graphics.renderOutline(selectorX(i) - 2, selectorY(i) - 2, 20, 20, GOLD);

        // ---- Filter slots of the selected tool ----
        List<SwapRules.Rule> mine = selectedRules;
        int hoveredFilter = selectedTool == null ? -1 : filterAt(mouseX, mouseY);
        for (int i = 0; i < SwapRulesMenu.FILTER_SLOTS; i++)
        {
            int fx = filterX(i);
            int fy = filterY(i);
            ToolScreen.drawSlot(graphics, fx - 1, fy - 1);
            if (i < mine.size())
            {
                SwapRules.Rule rule = mine.get(i);
                ItemStack icon = icon(rule.target());
                graphics.renderItem(icon, fx, fy);
                if (rule.mode() != SwapRules.Mode.ITEM)
                {
                    boolean tag = rule.mode() == SwapRules.Mode.TAG;
                    graphics.renderOutline(fx - 1, fy - 1, 18, 18, tag ? TAG_PURPLE : MOD_BLUE);
                    graphics.renderItemDecorations(font, icon, fx, fy, tag ? "#" : "@");
                }
            }
            if (i == hoveredFilter)
                graphics.fill(RenderType.guiOverlay(), fx, fy, fx + 16, fy + 16, HOVER);
        }

        // ---- Player inventory ----
        for (Slot slot : menu.slots)
            ToolScreen.drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
    }

    /** Title and section labels (coordinates relative to the GUI's corner). */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, statusTicks > 0 ? status : title, 23, 6, 0xFFFFFF, true);
        graphics.drawString(font, Component.translatable("gui.megatools.swap_rules.tools"),
                SwapRulesMenu.LEFT, SwapRulesMenu.SELECTOR_LABEL_Y, LABEL_COLOR, false);

        Entry selected = selectedEntry();
        if (selected == null)
            graphics.drawString(font, Component.translatable("gui.megatools.swap_rules.no_tools"),
                    SwapRulesMenu.LEFT, menu.filterLabelY(), HINT_COLOR, false);
        else
        {
            Component name = selected.stored() ? selected.icon().getHoverName() : itemName(selected.id());
            String label = Component.translatable("gui.megatools.swap_rules.filters_for", name).getString();
            int room = imageWidth - 2 * SwapRulesMenu.LEFT;
            if (font.width(label) > room)
                label = font.plainSubstrByWidth(label, room - font.width("...")) + "...";
            graphics.drawString(font, label, SwapRulesMenu.LEFT, menu.filterLabelY(), LABEL_COLOR, false);
        }
        graphics.drawString(font, playerInventoryTitle, SwapRulesMenu.LEFT, menu.invLabelY(), LABEL_COLOR, false);
    }
}
