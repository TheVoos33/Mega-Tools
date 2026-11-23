package com.thevoos.megatools;

import com.thevoos.megatools.client.ClientEvents;
import com.thevoos.megatools.client.DynamicLightsCompat;
import com.thevoos.megatools.client.AllthemodiumGuide;
import com.thevoos.megatools.client.ClientCommands;
import com.thevoos.megatools.client.FlatTextures;
import com.thevoos.megatools.client.MegaToolArmPose;
import com.thevoos.megatools.client.MegaToolClientExtensions;
import com.thevoos.megatools.client.ModKeyMappings;
import com.thevoos.megatools.client.ToolHudOverlay;
import com.thevoos.megatools.client.ToolScreen;
import com.thevoos.megatools.client.ToolStatsAccess;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.tool.MegaToolMining;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
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

        // Dynamic lights mods light a held tool themselves (then no light blocks are placed for this player).
        // Registered this early so the mods have it before they first collect their light sources.
        DynamicLightsCompat.init();

        // Tool GUI: size the toggle buttons to their labels with the client font (see GuiLayout).
        ToolMenu.setClientLayoutFactory(ToolScreen::createLayout);

        // Mods screen > Mega Tools > Config  (NeoForge's auto-generated config screen). The Tool Stats
        // section needs cheats / operator in a hosted world (see ToolStatsAccess).
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new ConfigurationScreen(mod, parent, ToolStatsAccess::section));

        // ---- Mod bus: one-time client registration -------------------------------------------
        modEventBus.addListener(ModKeyMappings::onRegisterKeyMappings);                 // Ctrl+R / Shift+R / R
        modEventBus.addListener(ClientEvents::onRegisterScreens);                       // ToolMenu -> ToolScreen
        modEventBus.addListener(ToolHudOverlay::onRegisterGuiLayers);                   // auto-swap icon near hotbar
        modEventBus.addListener(MegaToolClientExtensions::onRegisterClientExtensions);  // "prodding" animation
        modEventBus.addListener(AllthemodiumGuide::onAddPackFinders);                   // guide page, only with Allthemodium
        modEventBus.addListener(FlatTextures::onAddPackFinders);                        // built-in flat textures pack
        modEventBus.addListener(MegaToolArmPose::onRegisterRenderStateModifiers);       // third-person prod: poke state per player

        // ---- Game bus: per-tick / per-input client logic -------------------------------------
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickPre);   // marks "inside the game tick"
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientCommands::onRegisterClientCommands); // /megatools animation, menu...
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onInteractionKey); // Mega Tool right-click swing
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouseScroll);   // Shift + scroll between pre-selected tools
        NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);     // reload tool aliases
        NeoForge.EVENT_BUS.addListener(ClientEvents::onAttackEntity);  // Mega Tool poke on hits
    }
}
