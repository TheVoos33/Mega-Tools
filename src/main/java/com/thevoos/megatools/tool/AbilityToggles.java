package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The menu's ability toggles, also reachable by hotkey: one key binding per toggle (ModKeyMappings#TOGGLES,
 * unbound by default). The client sends ToggleAbilityPayload(bit) while a Mega Tools tool is in either hand;
 * the server flips the same ToolSettings bit as the menu button with that id (ToolMenu#clickMenuButton) on
 * that tool and says the new state above the hotbar.
 */
public final class AbilityToggles
{
    /**
     * Name of each toggle; index = ToolSettings bit = menu button id. Lang keys: gui.megatools.toggle.<name>
     * (label), key.megatools.toggle.<name> (key binding).
     */
    public static final String[] NAMES = {"strip", "path", "till", "shear", "brush", "ignite", "delayed_break",
            "place_torch", "light", "break", "attack"};

    /** Shear / Brush / Ignite only work while a Support Tool is in its slot. */
    public static final int SUPPORT_FLAGS = ToolSettings.SHEAR | ToolSettings.BRUSH | ToolSettings.IGNITE;
    /** Stored inverted, so fresh tools have them on. */
    private static final int INVERTED_FLAGS = ToolSettings.LIGHT_OFF | ToolSettings.BREAK_OFF | ToolSettings.ATTACK_OFF;

    private AbilityToggles() {}

    /** Whether toggle {@code bit} is switched on (Light, Break and Attack are stored inverted). */
    public static boolean isEnabled(ToolSettings settings, int bit)
    {
        return ((INVERTED_FLAGS & 1 << bit) != 0) != settings.has(1 << bit);
    }

    /** The hand holding a Mega Tools tool (paxel or Mega Tool): the main hand first, then the off hand; null if neither. */
    public static InteractionHand toolHand(Player player)
    {
        if (player.getMainHandItem().getItem() instanceof MultiToolItem)
            return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof MultiToolItem)
            return InteractionHand.OFF_HAND;
        return null;
    }

    /** Server: a toggle hotkey was pressed. Flips that toggle on the held tool, like its menu button. */
    public static void toggle(ServerPlayer player, int bit)
    {
        if (bit < 0 || bit >= NAMES.length)
            return;
        InteractionHand hand = toolHand(player);
        if (hand == null)
            return;
        ItemStack tool = player.getItemInHand(hand);
        Component name = Component.translatable("gui.megatools.toggle." + NAMES[bit]);

        if (!((MultiToolItem) tool.getItem()).getTier().hasToggle(bit))
        {
            player.displayClientMessage(Component.translatable("message.megatools.toggle_not_on_tool", name), true);
            return;
        }
        if ((SUPPORT_FLAGS & 1 << bit) != 0 && !ToolData.hasSupportTool(tool))
        {
            player.displayClientMessage(Component.translatable("message.megatools.toggle_needs_support", name), true);
            return;
        }

        ToolSettings settings = ToolData.getSettings(tool).toggle(1 << bit);
        ToolData.setSettings(tool, settings); // the changed tool is synced to the client with the inventory
        boolean on = isEnabled(settings, bit);
        player.displayClientMessage(Component.translatable("message.megatools.toggled", name,
                Component.translatable(on ? "gui.megatools.enabled" : "gui.megatools.disabled")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), true);
    }
}
