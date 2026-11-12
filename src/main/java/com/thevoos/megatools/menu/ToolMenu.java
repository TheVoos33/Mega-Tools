package com.thevoos.megatools.menu;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.tool.LightSources;
import com.thevoos.megatools.tool.ToolData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
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
    private final ToolTier tier;
    private final GuiLayout layout;
    private final SimpleContainer plates = new SimpleContainer(ToolData.PLATE_SLOTS);
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

    private final int lightIndex;
    private final int storageStart;
    private final int containerSlotCount;

    /** Client side factory - called by the MenuType with the extra data written in ServerPayloadHandler. */
    public static ToolMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData)
    {
        int toolSlot = extraData.readVarInt();
        return new ToolMenu(containerId, playerInventory, toolSlot);
    }

    /** Server constructor (and the real implementation). */
    public ToolMenu(int containerId, Inventory playerInventory, int toolSlot)
    {
        super(ModMenus.TOOL_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.toolSlot = Mth.clamp(toolSlot, 0, Inventory.getSelectionSize() - 1);

        ItemStack tool = playerInventory.getItem(this.toolSlot);
        this.tier = tool.getItem() instanceof MultiToolItem multiTool ? multiTool.getTier() : ToolTier.STONE;
        this.layout = GuiLayout.of(tier);
        this.storage = new SimpleContainer(tier.storageSlots());

        // ---- Load the tool's data into the temporary containers ----
        if (tier.hasEnchantPlates())
        {
            NonNullList<ItemStack> savedPlates = ToolData.getPlates(tool);
            for (int i = 0; i < ToolData.PLATE_SLOTS; i++) plates.setItem(i, savedPlates.get(i));
        }
        NonNullList<ItemStack> savedStorage = ToolData.getStorage(tool, tier.storageSlots());
        for (int i = 0; i < tier.storageSlots(); i++) storage.setItem(i, savedStorage.get(i));
        light.setItem(0, ToolData.getTorches(tool));

        // Listeners are added AFTER loading so loading doesn't trigger a save.
        plates.addListener(this::onContainerChanged);
        storage.addListener(this::onContainerChanged);
        light.addListener(this::onContainerChanged);

        // ---- Enchantment plate slots (right of the toggle buttons) ----
        if (tier.hasEnchantPlates())
        {
            addSlot(new PlateSlot(plates, ToolData.PRIMARY, layout.plateX(), layout.plateY0()));
            addSlot(new PlateSlot(plates, ToolData.SECONDARY, layout.plateX(), layout.plateY1()));
        }

        // ---- Light source slot (every tier) ----
        lightIndex = slots.size();
        addSlot(new LightSlot(light, 0, layout.lightX(), layout.lightY()));

        // ---- Wrench storage slots (bottom of the tool section, 8 per row) ----
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
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, layout.invY() + row * 18));

        // ---- Hotbar (the held tool is locked) ----
        for (int col = 0; col < 9; col++)
        {
            if (col == this.toolSlot)
                addSlot(new LockedSlot(playerInventory, col, 8 + col * 18, layout.hotbarY()));
            else
                addSlot(new Slot(playerInventory, col, 8 + col * 18, layout.hotbarY()));
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

    private void onContainerChanged(Container container)
    {
        ItemStack tool = getTool();
        if (!(tool.getItem() instanceof MultiToolItem))
            return;

        ToolData.setTorches(tool, light.getItem(0)); // ToolLightManager picks the change up next tick
        if (tier.storageSlots() > 0)
            ToolData.setStorage(tool, storage.getItems());
        if (tier.hasEnchantPlates())
        {
            ToolData.setPlates(tool, plates.getItems());
            ToolData.applyActivePlate(tool); // putting/removing a plate updates enchantments immediately
        }
    }

    // =====================================================================================
    //  Toggle buttons (button id = bit index in ToolSettings)
    // =====================================================================================

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        // stone: strip/path/till/torch, iron & diamond: + shear/brush/ignite, mega: + delayed break
        if (!tier.hasToggle(id))
            return false;

        ItemStack tool = getTool();
        if (!(tool.getItem() instanceof MultiToolItem))
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
            // Player inventory -> plate slots (plates), torch slot (torches) or storage (tools)
            boolean moved = false;
            if (tier.hasEnchantPlates() && stack.getItem() instanceof EnchantmentPlateItem)
                moved = moveItemStackTo(stack, 0, ToolData.PLATE_SLOTS, false);
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

    @Override
    public boolean stillValid(Player player)
    {
        return player.getInventory().selected == toolSlot && getTool().getItem() instanceof MultiToolItem;
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
     * Wrench storage filter.
     *
     * Never allowed (prevents recursive storage and item loss):
     *   Mega Tools tools, Enchantment Plates, tools that are currently swapped out.
     * Always allowed: anything in the #megatools:storable_tools item tag.
     * Otherwise allowed: unstackable items (tools, wrenches, hammers...) that don't hold other
     *   items (shulker boxes and bundles are refused to keep item data small).
     * Each slot holds one item (getMaxStackSize below), even for stackable tagged items.
     */
    public static class StorageSlot extends Slot
    {
        public StorageSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        public static boolean canStore(ItemStack stack)
        {
            // ---- hard rules, the tag can't override these ----
            if (stack.isEmpty()
                    || stack.getItem() instanceof MultiToolItem
                    || stack.getItem() instanceof EnchantmentPlateItem
                    || stack.has(ModDataComponents.SWAP_ORIGIN.get()))
                return false;

            // ---- explicitly allowed by the tag (e.g. stackable wrenches, labellers) ----
            if (stack.is(ModTags.STORABLE_TOOLS))
                return true;

            // ---- default rule ----
            return stack.getMaxStackSize() == 1
                    && !stack.has(DataComponents.CONTAINER)
                    && !stack.has(DataComponents.BUNDLE_CONTENTS);
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
