package com.thevoos.megatools;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.thevoos.megatools.items.ToolTier;
import com.thevoos.megatools.mixin.RangedAttributeAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.mojang.logging.LogUtils;
import com.thevoos.megatools.block.ModBlocks;
import com.thevoos.megatools.event.CommonEvents;
import com.thevoos.megatools.items.ModCreativeModeTab;
import com.thevoos.megatools.items.ModItems;
import com.thevoos.megatools.menu.ModMenus;
import com.thevoos.megatools.network.ModNetworking;
import com.thevoos.megatools.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
    // Mod id - must match the modId in META-INF/mods.toml
    public static final String MODID = "megatools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MegaTools()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ---- Startup config FIRST: tool stats (damage, durability, mining speed) are read when the
        //      items are built during registration, which on Forge happens before COMMON configs load,
        //      so this file is read right here. config/megatools-startup.toml, restart required after edits.
        StartupConfig.load();

        // ---- Registries (mod event bus) --------------------------------------------------------
        // Data components first: they describe the per-stack data (settings, storage, plates, swap info).
        ModBlocks.register(modEventBus);    // invisible light placed by tools holding a light source
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);   // crafting recipes that carry tool data over
        ModCreativeModeTab.register(modEventBus);

        // ---- Networking (mod event bus) --------------------------------------------------------
        // Client -> server packets for Ctrl+R, Shift+R, R, renaming and Mega Tool clicks.
        ModNetworking.register();

        // ---- Gameplay events (Forge game bus) -----------------------------------------------
        // Mega Tool mining speed/delay, anvil plate enchanting, swap safety net, etc.
        CommonEvents.register();

        // ---- Config ----------------------------------------------------------------------------
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // ---- Client-only setup (key binds, screens, HUD, render tweaks) ----------------------------
        if (FMLEnvironment.dist == Dist.CLIENT)
            MegaToolsClient.init(modEventBus);

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
        if (Attributes.ATTACK_DAMAGE instanceof RangedAttribute attribute && needed > attribute.getMaxValue())
        {
            LOGGER.info("Raising the attack damage attribute limit from {} to {} (megatools-startup.toml)",
                    attribute.getMaxValue(), needed);
            ((RangedAttributeAccessor) attribute).megatools$setMaxValue(needed);
        }
    }

    /** Small helper so every class builds ResourceLocations in the mod namespace the same way. */
    public static ResourceLocation id(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
