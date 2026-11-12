package com.thevoos.megatools;

import com.thevoos.megatools.client.ClientEvents;
import com.thevoos.megatools.client.MegaToolClientExtensions;
import com.thevoos.megatools.client.ModKeyMappings;
import com.thevoos.megatools.client.ToolHudOverlay;
import com.thevoos.megatools.tool.MegaToolMining;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entry point. This class is never loaded on a dedicated server, so it is
 * safe to reference rendering / input classes from here.
 *
 * Listeners are added explicitly (instead of with @EventBusSubscriber) so it is always clear
 * which bus each event belongs to.
 */
@Mod(value = MegaTools.MODID, dist = Dist.CLIENT)
public class MegaToolsClient
{
    public MegaToolsClient(IEventBus modEventBus, ModContainer container)
    {
        // Lets the common mining code ask "is the attack button held?" (see MegaToolMining), so only
        // real mining plays the Mega Tool animation / advances its hold delay.
        MegaToolMining.setClientAttackDownSupplier(() ->
        {
            Minecraft mc = Minecraft.getInstance();
            return mc.options != null && mc.screen == null && mc.options.keyAttack.isDown();
        });

        // Mods screen > Mega Tools > Config  (NeoForge's auto-generated config screen)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // ---- Mod bus: one-time client registration -------------------------------------------
        modEventBus.addListener(ModKeyMappings::onRegisterKeyMappings);                 // Ctrl+R / Shift+R / R
        modEventBus.addListener(ClientEvents::onRegisterScreens);                       // ToolMenu -> ToolScreen
        modEventBus.addListener(ToolHudOverlay::onRegisterGuiLayers);                   // auto-swap icon near hotbar
        modEventBus.addListener(MegaToolClientExtensions::onRegisterClientExtensions);  // "prodding" animation

        // ---- Game bus: per-tick / per-input client logic -------------------------------------
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickPre);   // marks "inside the game tick"
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseScroll);   // Shift + scroll between pre-selected tools
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);     // reload tool aliases
        NeoForge.EVENT_BUS.addListener(ClientEvents::onAttackEntity);  // Mega Tool poke on hits
        // HIGHEST so the GUI quick swap (hover a stored tool + R) runs before JEI's R key.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientEvents::onScreenKeyPressed);
    }
}
