package com.thevoos.megatools;

import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.mixin.RangedAttributeAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
        StartupConfig.update(); // files from Mega Tools 1.2 and older: new Mega Tool attack damage

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

        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(MegaTools::raiseAttackDamageLimit));
    }

    /**
     * Vanilla caps the attack damage attribute at 2048, and megatools-startup.toml allows up to 10,000.
     * If a tool is configured above the current limit, the limit is raised to that value (for the
     * attribute as a whole, like the AttributeFix mod does), otherwise the tool would silently hit for
     * 2048. With the default stats nothing is changed.
     */
    private static void raiseAttackDamageLimit()
    {
        double needed = 0.0D;
        for (ToolTier tier : ToolTier.values())
            needed = Math.max(needed, tier.attackDamage());
        if (Attributes.ATTACK_DAMAGE.value() instanceof RangedAttribute attribute && needed > attribute.getMaxValue())
        {
            LOGGER.info("Raising the attack damage attribute limit from {} to {} (megatools-startup.toml)",
                    attribute.getMaxValue(), needed);
            ((RangedAttributeAccessor) attribute).megatools$setMaxValue(needed);
        }
    }

    /** Small helper so every class builds ResourceLocations in the mod namespace the same way. */
    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
