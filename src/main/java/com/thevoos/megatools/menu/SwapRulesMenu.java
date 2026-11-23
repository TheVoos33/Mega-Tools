package com.thevoos.megatools.menu;

import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.ToolTier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container behind the Swap Rules GUI (opened with the "Custom Rule" button of the tool GUI).
 *
 * It only holds the player's inventory, so items can be picked up and dropped on a filter slot. The
 * stored tools and the filter slots are NOT menu slots: the screen draws them from the tool's data
 * components and sends every change as a SetSwapRulesPayload. Nothing ever leaves the inventory: a
 * filter only remembers which item was shown to it.
 *
 * Like ToolMenu, the held tool's hotbar slot is locked and the GUI closes if the tool is replaced.
 *
 * The pixel layout lives here because the inventory slot positions depend on it (both sides):
 *
 *   y=4    [<] Custom Rules .......................... [Copy] [Paste]   (title bar, drawn by the screen)
 *   y=20   Stored tools
 *   y=30   [ one cell per stored tool, 9 per row: 1 row iron, 2 rows diamond, 3 rows netherite, 4 rows mega ]
 *          Blocks for <selected tool> ................................ [Whitelist]   (toggles to Blacklist)
 *          [ 2 x 9 filter slots of the shown list ]
 *          Inventory ............................................ [Save the file]
 *          [ player inventory 3x9 ]
 *          [ hotbar 1x9 ]   (the held tool's slot is locked)
 */
public class SwapRulesMenu extends AbstractContainerMenu
{
    public static final int WIDTH = 176;
    public static final int COLUMNS = 9;
    public static final int FILTER_SLOTS = SwapRules.MAX_PER_TOOL;
    public static final int FILTER_ROWS = FILTER_SLOTS / COLUMNS;
    /** Item x of the first column (slot frames are drawn 1px outside the item position). */
    public static final int LEFT = 8;
    public static final int SELECTOR_LABEL_Y = 20;
    public static final int SELECTOR_Y = 30;

    private final Player player;
    private final int toolSlot;
    /** The exact stack the GUI was opened for (see ToolMenu#openedTool). */
    private final ItemStack openedTool;
    private final ToolTier tier;

    private final int selectorRows;
    private final int filterLabelY;
    private final int filterY;
    private final int invLabelY;
    private final int height;

    /** Client side factory: reads the tool's hotbar slot written in ServerPayloadHandler. */
    public static SwapRulesMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf extraData)
    {
        return new SwapRulesMenu(containerId, playerInventory, extraData.readVarInt());
    }

    public SwapRulesMenu(int containerId, Inventory playerInventory, int toolSlot)
    {
        super(ModMenus.SWAP_RULES_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.toolSlot = Mth.clamp(toolSlot, 0, Inventory.getSelectionSize() - 1);
        this.openedTool = playerInventory.getItem(this.toolSlot);
        this.tier = openedTool.getItem() instanceof MultiToolItem multiTool ? multiTool.getTier() : ToolTier.STONE;

        // ---- Vertical layout ----
        selectorRows = Math.max(1, (tier.storageSlots() + COLUMNS - 1) / COLUMNS);
        filterLabelY = SELECTOR_Y + selectorRows * 18 + 4;
        filterY = filterLabelY + 13; // room for the 12px Whitelist / Blacklist button on the label row
        invLabelY = filterY + FILTER_ROWS * 18 + 4;
        int invY = invLabelY + 13; // room for the 12px Save the file button on the label row
        int hotbarY = invY + 3 * 18 + 4;
        height = hotbarY + 18 + 6;

        // ---- Player inventory ----
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, LEFT + col * 18, invY + row * 18));

        // ---- Hotbar (the held tool is locked) ----
        for (int col = 0; col < 9; col++)
        {
            if (col == this.toolSlot)
                addSlot(new ToolMenu.LockedSlot(playerInventory, col, LEFT + col * 18, hotbarY));
            else
                addSlot(new Slot(playerInventory, col, LEFT + col * 18, hotbarY));
        }
    }

    /** Always look the tool up fresh: synced stacks are replaced, so a cached reference goes stale. */
    public ItemStack getTool()
    {
        return player.getInventory().getItem(toolSlot);
    }

    public ToolTier getTier() { return tier; }
    /** Cells in the stored-tool selector (9 per row, as many rows as the tool GUI's storage). */
    public int selectorCells() { return selectorRows * COLUMNS; }
    public int filterLabelY() { return filterLabelY; }
    public int filterY() { return filterY; }
    public int invLabelY() { return invLabelY; }
    public int height() { return height; }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
        // Pressing the number key of the tool's own hotbar slot would swap the tool out - block it.
        if (clickType == ClickType.SWAP && button == toolSlot)
            return;
        super.clicked(slotId, button, clickType, player);
    }

    /** There is nowhere to move items to. (The screen turns a Shift-click into "add as filter".) */
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    /** Server only (vanilla checks this every tick). The same tool stack must still be held. */
    @Override
    public boolean stillValid(Player player)
    {
        return player.getInventory().selected == toolSlot && getTool() == openedTool
                && openedTool.getItem() instanceof MultiToolItem multiTool && multiTool.getTier().hasAutoSwap();
    }
}
