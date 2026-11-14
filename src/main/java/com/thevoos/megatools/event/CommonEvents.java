package com.thevoos.megatools.event;

import com.thevoos.megatools.command.MegaToolsCommand;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.recipe.ToolCarryOver;
import com.thevoos.megatools.tool.BrokenToolHandler;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.ToolLightManager;
import com.thevoos.megatools.tool.PlateHelper;
import com.thevoos.megatools.tool.ToolSwapper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

/** Registers all gameplay listeners on the NeoForge (game) event bus. Runs on both sides. */
public final class CommonEvents
{
    private CommonEvents() {}

    public static void register()
    {
        // Mega Tool: instant break, hold delay, fresh-click bookkeeping
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onAttackEntity);   // third-person poke for others
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onLogout);

        // Enchantment Plate + Enchanted Book in an anvil
        NeoForge.EVENT_BUS.addListener(PlateHelper::onAnvilUpdate);
        // Which enchantments plates accept is cached per enchantment; tags changed -> recompute
        NeoForge.EVENT_BUS.addListener(EnchantmentPlateItem::onTagsUpdated);

        // Return the Mega Tools tool if a swapped-out tool breaks
        NeoForge.EVENT_BUS.addListener(ToolSwapper::onItemDestroyed);

        // Broken tools (1 durability left) can't mine or attack until repaired
        NeoForge.EVENT_BUS.addListener(BrokenToolHandler::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(BrokenToolHandler::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BrokenToolHandler::onBreakSpeed); // after MegaToolMining

        // Held-tool lighting: move the invisible light with the player, clean it up when needed
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onServerTick);      // fade out replaced lights
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onLogout);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onDeath);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onChunkSent);       // re-send light to clients still loading chunks
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onChunkUnwatch);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onBlockPlaced);     // lights in a portal frame would stop it from lighting

        // Give back stored tools/plates if a tool is consumed by a non-carry-over crafting recipe
        NeoForge.EVENT_BUS.addListener(ToolCarryOver::onItemCrafted);

        // Tell joining players the server's tool stats (startup config isn't synced)
        NeoForge.EVENT_BUS.addListener(com.thevoos.megatools.network.ToolStatsPayload::onPlayerLoggedIn);

        // Storage whitelist file: read on server start and /reload, sent to joining players
        NeoForge.EVENT_BUS.addListener(com.thevoos.megatools.tool.StorageWhitelist::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(com.thevoos.megatools.tool.StorageWhitelist::onDatapackSync);

        // /megatools commands (kit)
        NeoForge.EVENT_BUS.addListener(MegaToolsCommand::onRegisterCommands);
    }
}
