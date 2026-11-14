package com.thevoos.megatools.menu;

import com.thevoos.megatools.items.ToolTier;

/**
 * Pixel layout of the tool GUI, shared by ToolMenu (slot positions, both sides) and
 * ToolScreen (drawing). Computed per tier, so each tool gets its own GUI size:
 *
 *   y=6    [ editable tool name (title bar) ......................................... ]
 *   y=18          [Strip ] [Path  ] [Till  ]
 *   y=36   [Supp] [Shear ] [Brush ] [Ignite] [Plate 1]
 *   y=54   [Torc] [Delayed Br] [Place Torch] [Plate 2]   (mega; others: [    Place Torch    ])
 *   y=72   [ok]   small light on/off button under the torch slot (ToolScreen; not part of the grid)
 *   Each slot sits next to the buttons it powers: the Support Tool slot unlocks Shear / Brush /
 *   Ignite (the buttons are hidden while it's empty), the torch slot feeds Place Torch and the
 *   held-tool light. The two enchantment plate slots mirror those two
 *   slots on the right: same rows, same gap to the buttons, same margin to the panel edge.
 *   The last row's buttons share the grid's exact width, so its outer edges line up with the
 *   rows above and the gap between them equals the grid gap.
 *          Tool Storage
 *          [ 9 slots per row, in line with the inventory columns: 1 row iron, 2 rows diamond, 3 rows mega ]
 *          Inventory
 *          [ player inventory 3x9 ]
 *          [ hotbar 1x9 ]   (the held tool's slot is locked)
 *
 * WIDTH: the toggle buttons are made wide enough for their full label (no scrolling text), and the
 * plate column sits just after them. The GUI is never narrower than vanilla's 176; when it is wider
 * than its contents, the top section, storage and the inventory are each centred. Label widths come from the client font (ToolScreen), so
 * translations and resource-pack fonts fit too. The server doesn't know the font and uses the default
 * widths - fine, because the server never uses slot pixel positions.
 *
 * All coordinates are relative to the GUI's top-left corner; slot coordinates are the
 * 16x16 item position (the slot frame is drawn 1px outside it).
 */
public record GuiLayout(int width, int toggleRows, int toggleX, int toggleW, int lastRowToggleW, int gridW,
                        int plateX, int plateY0, int plateY1, int lightX, int lightY, int supportX, int supportY,
                        int storageX, int storageLabelY, int storageY, int storageRows,
                        int storageLabelX, int invLabelX, int invX, int invLabelY, int invY, int hotbarY, int height)
{
    public static final int MIN_WIDTH = 176;
    /** Same as the player inventory, so storage and inventory slots line up column for column. */
    public static final int STORAGE_COLUMNS = 9;
    public static final int TOGGLE_GAP = 2;
    public static final int TOP = 18;
    public static final int ROW = 18;
    /** Support Tool row (Shear / Brush / Ignite). */
    public static final int SUPPORT_ROW = 1;
    /** Panel edge to a side slot's frame (the frame is drawn 1px outside the item position). */
    private static final int SIDE_MARGIN = 7;
    /** Item x of the slot column left of the buttons (torch, Support Tool). */
    private static final int SLOT_COLUMN_X = SIDE_MARGIN + 1;
    /** Pixels between a side slot's frame and the buttons, the same on both sides. */
    private static final int SLOT_BUTTON_GAP = 3;
    /** Buttons start after the left slot column: frame (18) + gap. */
    private static final int TOGGLE_X = SIDE_MARGIN + 18 + SLOT_BUTTON_GAP;
    private static final int MIN_TOGGLE_W = 44;
    /** Space around a label inside its button (vanilla starts scrolling 2px from each edge). */
    private static final int LABEL_PADDING = 8;

    /** Default widths (server side, or when the label widths aren't known). */
    public static GuiLayout of(ToolTier tier)
    {
        return of(tier, 0, 0, 0);
    }

    /**
     * @param gridLabelW    widest label of the 3-wide toggle grid, in pixels
     * @param lastRowLabelW widest label of the last row (Delayed Break / Place Torch), in pixels
     * @param rowTextW      widest text drawn across a whole grid row (the "insert a Support Tool" hint)
     */
    public static GuiLayout of(ToolTier tier, int gridLabelW, int lastRowLabelW, int rowTextW)
    {
        // ---- Toggle buttons: grid cells fit the widest grid label, last row fits its widest label ----
        int lastRowCount = tier.hasToggle(6) ? 2 : 1;
        int cellW = Math.max(MIN_TOGGLE_W, gridLabelW + LABEL_PADDING);
        int lastRowNeeded = lastRowCount * (lastRowLabelW + LABEL_PADDING) + (lastRowCount - 1) * TOGGLE_GAP;
        int gridW = Math.max(Math.max(3 * cellW + 2 * TOGGLE_GAP, lastRowNeeded), rowTextW + LABEL_PADDING);
        cellW = (gridW - 2 * TOGGLE_GAP + 2) / 3; // stretch the cells (rounded up) so both rows end at the same x
        // Two last-row buttons split (grid - gap) exactly only if it is even, i.e. if cellW is even.
        if (lastRowCount == 2 && cellW % 2 != 0)
            cellW++;
        gridW = 3 * cellW + 2 * TOGGLE_GAP;
        int lastRowToggleW = (gridW - (lastRowCount - 1) * TOGGLE_GAP) / lastRowCount;

        // ---- Plate column (mirror of the left slot column), overall width ----
        int gridEnd = TOGGLE_X + gridW;
        int contentW = tier.hasEnchantPlates()
                ? gridEnd + SLOT_BUTTON_GAP + 18 + SIDE_MARGIN
                : gridEnd + SIDE_MARGIN;
        int width = Math.max(MIN_WIDTH, contentW);
        int topShift = (width - contentW) / 2;  // centres the top section when the panel is wider
        int toggleX = TOGGLE_X + topShift;
        int slotColumnX = SLOT_COLUMN_X + topShift;
        int plateX = toggleX + gridW + SLOT_BUTTON_GAP + 1;
        int offset = (width - MIN_WIDTH) / 2;   // centres the 176-wide storage + inventory block

        // ---- Vertical layout ----
        // 3-wide grid of ability toggles, plus a last row for Place Torch (and Delayed Break on the Mega Tool).
        int toggleRows = tier.toggleRows();
        int afterToggles = TOP + toggleRows * ROW + 2;
        int lightY = TOP + (toggleRows - 1) * ROW;
        int supportY = TOP + SUPPORT_ROW * ROW;

        int storageRows = (tier.storageSlots() + STORAGE_COLUMNS - 1) / STORAGE_COLUMNS;
        int storageLabelY = afterToggles;
        int storageY = storageLabelY + 10;
        int cursor = storageRows > 0 ? storageY + storageRows * 18 + 4 : afterToggles;

        int invLabelY = cursor;
        int invY = cursor + 10;
        int hotbarY = invY + 3 * 18 + 4;
        int height = hotbarY + 18 + 6;

        int invX = offset + 8;
        int storageX = invX; // 9 wide like the inventory: same columns
        // The first label under the buttons shares its row with the light button under the torch slot:
        // start it no further left than the buttons so they never overlap.
        int firstLabelX = Math.max(invX, toggleX);
        int storageLabelX = firstLabelX;
        int invLabelX = storageRows > 0 ? invX : firstLabelX;
        // Plates mirror the left column: primary beside the Support Tool slot, secondary beside the torch slot.
        return new GuiLayout(width, toggleRows, toggleX, cellW, lastRowToggleW, gridW,
                plateX, supportY, lightY, slotColumnX, lightY, slotColumnX, supportY,
                storageX, storageLabelY, storageY, storageRows,
                storageLabelX, invLabelX, invX, invLabelY, invY, hotbarY, height);
    }
}
