package com.thevoos.megatools.menu;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.SupportToolItem;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.tool.LightSources;
import com.thevoos.megatools.tool.StorageWhitelist;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/**
 * Server + client container for the tool GUI (opened with Ctrl + R).
 *
 * The tool itself stays in the player's hotbar; its storage and plates are copied into two
 * SimpleContainers. Whenever one of those changes, the contents are written back into the tool's
 * data components (save()). The held tool's hotbar slot is locked so it can't be moved while open.
 *
 * Ability toggles use vanilla's "menu button" packet: the screen calls
 * gameMode.handleInventoryButtonClick(containerId, id), the server runs clickMenuButton(id).
 */
public class ToolMenu extends AbstractContainerMenu
{
    private final Player player;
    private final int toolSlot;          // hotbar index of the held tool
    /**
     * The exact tool stack the GUI was opened for. If anything replaces it (another stack object in that
     * slot), the GUI closes: the containers below hold that tool's contents, and writing them into a
     * different stack, or taking items out of them, would duplicate items.
     */
    private final ItemStack openedTool;
    private final ToolTier tier;
    private final GuiLayout layout;
    private final SimpleContainer plates = new SimpleContainer(ToolData.PLATE_SLOTS);
    private final SimpleContainer support = new SimpleContainer(1);
    private final SimpleContainer storage;
    /** Torch slot: a one-slot container that allows up to 256 items instead of the usual 64/99. */
    private final SimpleContainer light = new SimpleContainer(1)
    {
        @Override
        public int getMaxStackSize()
        {
            return StoredTorches.MAX;
        }

        @Override
        public int getMaxStackSize(ItemStack stack)
        {
            return StoredTorches.MAX;
        }
    };

    private final int supportIndex;     // -1 if the tier has no Support Tool slot
    private final int lightIndex;
    private final int storageStart;
    private final int containerSlotCount;

    /**
     * Client: builds the layout with the real label widths (set by MegaToolsClient, because only the
     * client has a font). The server keeps GuiLayout's defaults: it never uses slot pixel positions.
     */
    private static Function<ToolTier, GuiLayout> clientLayoutFactory = GuiLayout::of;

    public static void setClientLayoutFactory(Function<ToolTier, GuiLayout> factory) { clientLayoutFactory = factory; }

    /** Client side factory - called by the MenuType with the extra data written in ServerPayloadHandler. */
    public static ToolMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData)
    {
        int toolSlot = extraData.readVarInt();
        return new ToolMenu(containerId, playerInventory, toolSlot, clientLayoutFactory);
    }

    /** Server constructor. */
    public ToolMenu(int containerId, Inventory playerInventory, int toolSlot)
    {
        this(containerId, playerInventory, toolSlot, GuiLayout::of);
    }

    private ToolMenu(int containerId, Inventory playerInventory, int toolSlot, Function<ToolTier, GuiLayout> layoutFactory)
    {
        super(ModMenus.TOOL_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.toolSlot = Mth.clamp(toolSlot, 0, Inventory.getSelectionSize() - 1);

        ItemStack tool = playerInventory.getItem(this.toolSlot);
        this.openedTool = tool;
        this.tier = tool.getItem() instanceof MultiToolItem multiTool ? multiTool.getTier() : ToolTier.STONE;
        this.layout = layoutFactory.apply(tier);
        this.storage = new SimpleContainer(tier.storageSlots());

        // ---- Load the tool's data into the temporary containers ----
        if (tier.hasEnchantPlates())
        {
            ToolData.syncLoadedPlate(tool); // enchantments removed from the tool by other mods leave the plate too
            NonNullList<ItemStack> savedPlates = ToolData.getPlates(tool);
            for (int i = 0; i < ToolData.PLATE_SLOTS; i++) plates.setItem(i, savedPlates.get(i));
        }
        NonNullList<ItemStack> savedStorage = ToolData.getStorage(tool, tier.storageSlots());
        for (int i = 0; i < tier.storageSlots(); i++) storage.setItem(i, savedStorage.get(i));
        light.setItem(0, ToolData.getTorches(tool));
        if (tier.supportTools())
            support.setItem(0, ToolData.getSupportTool(tool));

        // Listeners are added AFTER loading so loading doesn't trigger a save.
        plates.addListener(this::onContainerChanged);
        support.addListener(this::onContainerChanged);
        storage.addListener(this::onContainerChanged);
        light.addListener(this::onContainerChanged);

        // ---- Enchantment plate slots (right of the toggle buttons) ----
        if (tier.hasEnchantPlates())
        {
            addSlot(new PlateSlot(plates, ToolData.PRIMARY, layout.plateX(), layout.plateY0()));
            addSlot(new PlateSlot(plates, ToolData.SECONDARY, layout.plateX(), layout.plateY1()));
        }

        // ---- Support Tool slot (left of the Shear / Brush / Ignite buttons) ----
        supportIndex = tier.supportTools() ? slots.size() : -1;
        if (tier.supportTools())
            addSlot(new SupportSlot(support, 0, layout.supportX(), layout.supportY()));

        // ---- Torch slot (every tier, left of the Place Torch button) ----
        lightIndex = slots.size();
        addSlot(new LightSlot(light, 0, layout.lightX(), layout.lightY()));

        // ---- Wrench storage slots (bottom of the tool section, 9 per row like the inventory) ----
        storageStart = slots.size();
        for (int i = 0; i < tier.storageSlots(); i++)
        {
            int x = layout.storageX() + (i % GuiLayout.STORAGE_COLUMNS) * 18;
            int y = layout.storageY() + (i / GuiLayout.STORAGE_COLUMNS) * 18;
            addSlot(new StorageSlot(storage, i, x, y));
        }
        containerSlotCount = slots.size();

        // ---- Player inventory ----
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, layout.invX() + col * 18, layout.invY() + row * 18));

        // ---- Hotbar (the held tool is locked) ----
        for (int col = 0; col < 9; col++)
        {
            if (col == this.toolSlot)
                addSlot(new LockedSlot(playerInventory, col, layout.invX() + col * 18, layout.hotbarY()));
            else
                addSlot(new Slot(playerInventory, col, layout.invX() + col * 18, layout.hotbarY()));
        }
    }

    // =====================================================================================
    //  Accessors used by the screen
    // =====================================================================================

    /** Always look the tool up fresh: synced stacks are replaced, so a cached reference goes stale. */
    public ItemStack getTool()
    {
        return player.getInventory().getItem(toolSlot);
    }

    public ToolTier getTier() { return tier; }
    public GuiLayout getLayout() { return layout; }
    public int getToolSlot() { return toolSlot; }

    // =====================================================================================
    //  Saving
    // =====================================================================================

    /**
     * Writes back only the container that changed: moving a torch no longer re-copies all 27 stored
     * tools (and re-sends them to the client), and the enchantments are only reloaded when a plate moved.
     */
    private void onContainerChanged(Container container)
    {
        ItemStack tool = getTool();
        if (!(tool.getItem() instanceof MultiToolItem))
            return;

        if (container == light)
            ToolData.setTorches(tool, light.getItem(0));         // ToolLightManager picks the change up next tick
        else if (container == support)
            ToolData.setSupportTool(tool, support.getItem(0));   // abilities + their buttons follow immediately
        else if (container == storage)
            ToolData.setStorage(tool, storage.getItems());
        else if (container == plates)
        {
            ToolData.setPlates(tool, plates.getItems());
            ToolData.applyActivePlate(tool); // putting/removing a plate updates enchantments immediately
        }
    }

    // =====================================================================================
    //  Toggle buttons (button id = bit index in ToolSettings)
    // =====================================================================================

    private static final int SUPPORT_FLAGS = ToolSettings.SHEAR | ToolSettings.BRUSH | ToolSettings.IGNITE;

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        // all tiers: strip/path/till, shear/brush/ignite, place torch, light; mega: + delayed break
        if (!tier.hasToggle(id))
            return false;

        ItemStack tool = getTool();
        if (!(tool.getItem() instanceof MultiToolItem))
            return false;
        // Shear / Brush / Ignite only exist while a Support Tool is in its slot.
        if ((1 << id & SUPPORT_FLAGS) != 0 && !ToolData.hasSupportTool(tool))
            return false;

        ToolData.setSettings(tool, ToolData.getSettings(tool).toggle(1 << id));
        return true; // the changed tool is synced to the client by broadcastChanges()
    }

    // =====================================================================================
    //  Click handling / shift-click
    // =====================================================================================

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
        // Pressing the number key of the tool's own hotbar slot would swap the tool out - block it.
        if (clickType == ClickType.SWAP && button == toolSlot)
            return;
        // A number key / the off-hand key moves a slot's WHOLE stack into the inventory, past LightSlot#remove.
        // More torches than one normal stack must leave by clicking (64 at a time): an over-sized stack in
        // the inventory can't be saved.
        if (clickType == ClickType.SWAP && slotId == lightIndex)
        {
            ItemStack torches = light.getItem(0);
            if (torches.getCount() > torches.getMaxStackSize())
                return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int invStart = containerSlotCount;
        int invEnd = slots.size();

        if (index < containerSlotCount)
        {
            // Tool slots -> player inventory
            if (!moveItemStackTo(stack, invStart, invEnd, true))
                return ItemStack.EMPTY;
        }
        else
        {
            // Player inventory -> plate slots (plates), Support Tool slot, torch slot (torches) or storage (tools)
            boolean moved = false;
            if (tier.hasEnchantPlates() && stack.getItem() instanceof EnchantmentPlateItem)
                moved = moveItemStackTo(stack, 0, ToolData.PLATE_SLOTS, false);
            if (!moved && supportIndex >= 0 && stack.getItem() instanceof SupportToolItem)
                moved = moveItemStackTo(stack, supportIndex, supportIndex + 1, false);
            if (!moved && LightSources.isTorch(stack))
                moved = moveItemStackTo(stack, lightIndex, lightIndex + 1, false);
            if (!moved && StorageSlot.canStore(stack))
                moved = moveItemStackTo(stack, storageStart, containerSlotCount, false);
            if (!moved)
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        if (stack.getCount() == original.getCount())
            return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return original;
    }

    /** Server only (vanilla checks this every tick). The same tool stack must still be held. */
    @Override
    public boolean stillValid(Player player)
    {
        return player.getInventory().selected == toolSlot && getTool() == openedTool
                && openedTool.getItem() instanceof MultiToolItem;
    }

    // =====================================================================================
    //  Slot types
    // =====================================================================================

    /** Accepts only Enchantment Plates. */
    public static class PlateSlot extends Slot
    {
        public PlateSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return stack.getItem() instanceof EnchantmentPlateItem;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }

    /** Accepts only the Support Tool. */
    public static class SupportSlot extends Slot
    {
        public SupportSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return stack.getItem() instanceof SupportToolItem;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }

    /**
     * Torch slot: only #megatools:torches, up to 256. Taking items out gives at most one normal stack
     * (64) at a time, so an over-sized stack never ends up on the cursor, in the inventory or on the ground.
     */
    public static class LightSlot extends Slot
    {
        public LightSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return LightSources.isTorch(stack);
        }

        @Override
        public int getMaxStackSize()
        {
            return StoredTorches.MAX;
        }

        @Override
        public int getMaxStackSize(ItemStack stack)
        {
            return StoredTorches.MAX;
        }

        @Override
        public ItemStack remove(int amount)
        {
            ItemStack inSlot = getItem();
            return super.remove(inSlot.isEmpty() ? amount : Math.min(amount, inSlot.getMaxStackSize()));
        }
    }

    /**
     * Tool storage filter. Only items that are known to be tools are allowed, so the storage can't
     * be used as a general backpack.
     *
     * Never allowed (prevents recursive storage and item loss):
     *   Mega Tools tools, Enchantment Plates, tools that are currently swapped out.
     * Allowed:
     *   #c:tools/wrench, #megatools:storable_item (which includes shears, screwdrivers, crowbars...),
     *   and items listed in config/megatools-storage-whitelist.json for mods that are installed.
     * Each slot holds one item (getMaxStackSize below).
     */
    public static class StorageSlot extends Slot
    {
        public StorageSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        public static boolean canStore(ItemStack stack)
        {
            // ---- hard rules, nothing can override these ----
            if (stack.isEmpty()
                    || stack.getItem() instanceof MultiToolItem
                    || stack.getItem() instanceof EnchantmentPlateItem
                    || stack.has(ModDataComponents.SWAP_ORIGIN.get()))
                return false;

            return stack.is(ModTags.WRENCHES)
                    || stack.is(ModTags.STORABLE_ITEM)
                    || StorageWhitelist.contains(stack);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return canStore(stack);
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }

    /** The hotbar slot holding the tool whose GUI is open: can't be taken or replaced. */
    public static class LockedSlot extends Slot
    {
        public LockedSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return false;
        }
    }
}
