package com.thevoos.megatools.menu;

import com.thevoos.megatools.items.ToolTier;

/**
 * Pixel layout of the tool GUI, shared by ToolMenu (slot positions, both sides) and
 * ToolScreen (drawing). Computed per tier, so each tool gets its own GUI size:
 *
 *   y=6    [ editable tool name (title bar) .................................... ]
 *   y=18   [Strip] [Path ] [Till ]                                     [Plate 1]
 *   y=36   [Shear] [Brush] [Ignite]   (iron/diamond/mega only)         [Plate 2]
 *   y=54   [Delayed Brk][Place Torch]  (mega; others: [ Place Torch ])  [Torches]
 *   Right column: plates (iron/diamond/mega) then the light slot (all tiers). The stone paxel has
 *   no plates, so its light slot is at y=18.
 *          Tool Storage
 *          [ 8 slots per row: 1 row iron, 2 rows diamond, 3 rows mega ]
 *          Inventory
 *          [ player inventory 3x9 ]
 *          [ hotbar 1x9 ]   (the held tool's slot is locked)
 *
 * All coordinates are relative to the GUI's top-left corner; slot coordinates are the
 * 16x16 item position (the slot frame is drawn 1px outside it).
 */
public record GuiLayout(int toggleRows, int plateX, int plateY0, int plateY1, int lightX, int lightY,
                        int storageX, int storageLabelY, int storageY, int storageRows,
                        int invLabelY, int invY, int hotbarY, int height)
{
    public static final int WIDTH = 176;
    public static final int STORAGE_COLUMNS = 8;

    public static GuiLayout of(ToolTier tier)
    {
        // 3-wide grid of ability toggles, plus a last row for Place Torch (and Delayed Break on the Mega Tool).
        int toggleRows = tier.toggleRows();
        // The top section is as tall as the taller of the toggle grid and the right column.
        int rightColumnSlots = (tier.hasEnchantPlates() ? 2 : 0) + 1;
        int topRows = Math.max(toggleRows, rightColumnSlots);
        int afterToggles = 18 + topRows * 18 + 2;
        int lightY = 18 + (tier.hasEnchantPlates() ? 2 * 18 : 0);

        int storageRows = (tier.storageSlots() + STORAGE_COLUMNS - 1) / STORAGE_COLUMNS;
        int storageLabelY = afterToggles;
        int storageY = storageLabelY + 10;
        int cursor = storageRows > 0 ? storageY + storageRows * 18 + 4 : afterToggles;

        int invLabelY = cursor;
        int invY = cursor + 10;
        int hotbarY = invY + 3 * 18 + 4;
        int height = hotbarY + 18 + 6;

        int storageX = (WIDTH - STORAGE_COLUMNS * 18) / 2;
        return new GuiLayout(toggleRows, 152, 18, 36, 152, lightY, storageX, storageLabelY, storageY, storageRows,
                invLabelY, invY, hotbarY, height);
    }
}
