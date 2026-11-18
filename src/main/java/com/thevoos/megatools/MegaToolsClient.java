package com.thevoos.megatools;

import com.thevoos.megatools.client.ClientEvents;
import com.thevoos.megatools.client.DynamicLightsCompat;
import com.thevoos.megatools.client.FlatTextures;
import com.thevoos.megatools.client.MegaToolArmPose;
import com.thevoos.megatools.client.ModKeyMappings;
import com.thevoos.megatools.client.ToolHudOverlay;
import com.thevoos.megatools.client.ToolScreen;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.tool.MegaToolMining;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client-only setup, called by MegaTools on the physical client only. This class is never loaded on a
 * dedicated server, so it is safe to reference rendering / input classes from here.
 *
 * Listeners are added explicitly (instead of with @EventBusSubscriber) so it is always clear
 * which bus each event belongs to.
 */
public final class MegaToolsClient
{
    private MegaToolsClient() {}

    public static void init(IEventBus modEventBus)
    {
        // Lets the common mining code ask "is the attack button held?" (see MegaToolMining), so only
        // real mining plays the Mega Tool animation / advances its hold delay.
        MegaToolMining.setClientAttackDownSupplier(() ->
        {
            Minecraft mc = Minecraft.getInstance();
            return mc.options != null && mc.screen == null && mc.options.keyAttack.isDown();
        });

        // Dynamic lights mods light a held tool themselves (then no light blocks are placed for this player).
        // Registered this early so the mods have it before they first collect their light sources.
        DynamicLightsCompat.init();

        // Tool GUI: size the toggle buttons to their labels with the client font (see GuiLayout).
        ToolMenu.setClientLayoutFactory(ToolScreen::createLayout);

        // Third-person poke pose: must be added to the ArmPose enum before any player model is drawn
        // (see MegaToolArmPose#init).
        MegaToolArmPose.init();

        // ---- Mod bus: one-time client registration -------------------------------------------
        modEventBus.addListener(ModKeyMappings::onRegisterKeyMappings);                 // Ctrl+R / Shift+R / R
        modEventBus.addListener(ClientEvents::onClientSetup);                           // ToolMenu -> ToolScreen
        modEventBus.addListener(ToolHudOverlay::onRegisterGuiOverlays);                 // auto-swap icon near hotbar
        modEventBus.addListener(FlatTextures::onAddPackFinders);                        // built-in flat textures pack
        modEventBus.addListener(FlatTextures::onRegisterReloadListeners);               // poke animation on/off

        // ---- Game bus: per-tick / per-input client logic -------------------------------------
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onClientTickPre);   // marks "inside the game tick"
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onMouseButton);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onInteractionKey); // Mega Tool right-click swing
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onMouseScroll);   // Shift + scroll between pre-selected tools
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);     // reload tool aliases
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onAttackEntity);  // Mega Tool poke on hits
    }
}
