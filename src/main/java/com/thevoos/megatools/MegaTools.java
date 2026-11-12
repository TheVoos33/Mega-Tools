package com.thevoos.megatools;

import com.mojang.logging.LogUtils;
import com.thevoos.megatools.block.ModBlocks;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.event.CommonEvents;
import com.thevoos.megatools.items.ModCreativeModeTab;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.menu.ModMenus;
import com.thevoos.megatools.network.ModNetworking;
import com.thevoos.megatools.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Common entry point for Mega Tools (runs on both the client and the dedicated server).
 *
 * Everything that must exist on both sides is wired up here: data components, items,
 * the tool menu type, the creative tab, network payloads, game-bus event listeners and the config.
 * Client-only wiring (key binds, screens, HUD, render tweaks) lives in {@link MegaToolsClient}.
 */
@Mod(MegaTools.MODID)
public class MegaTools
{
    // Mod id - must match the modId in META-INF/neoforge.mods.toml
    public static final String MODID = "megatools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MegaTools(IEventBus modEventBus, ModContainer modContainer)
    {
        // ---- Startup config FIRST: tool stats (damage, durability, mining speed) are read when the
        //      items are built during registration, so this file must be loaded before that.
        //      config/megatools-startup.toml, restart required after edits.
        modContainer.registerConfig(ModConfig.Type.STARTUP, StartupConfig.SPEC);

        // ---- Registries (mod event bus) --------------------------------------------------------
        // Data components first: they describe the per-stack data (settings, storage, plates, swap info).
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);    // invisible light placed by tools holding a light source
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);   // crafting recipes that carry tool data over
        ModCreativeModeTab.register(modEventBus);

        // ---- Networking (mod event bus) --------------------------------------------------------
        // Client -> server packets for Ctrl+R, Shift+R, R, renaming and Mega Tool clicks.
        modEventBus.addListener(ModNetworking::onRegisterPayloads);

        // ---- Gameplay events (NeoForge game bus) -----------------------------------------------
        // Mega Tool mining speed/delay, anvil plate enchanting, swap safety net, etc.
        CommonEvents.register();

        // ---- Config ----------------------------------------------------------------------------
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Small helper so every class builds ResourceLocations in the mod namespace the same way. */
    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
