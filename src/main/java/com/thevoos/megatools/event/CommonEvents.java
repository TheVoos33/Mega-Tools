package com.thevoos.megatools.event;

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
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onAttackEntity);   // third-person poke for others   // #megatools:mega_tool_unbreakable
        NeoForge.EVENT_BUS.addListener(MegaToolMining::onLogout);

        // Enchantment Plate + Enchanted Book in an anvil
        NeoForge.EVENT_BUS.addListener(PlateHelper::onAnvilUpdate);

        // Return the Mega Tools tool if a swapped-out tool breaks
        NeoForge.EVENT_BUS.addListener(ToolSwapper::onItemDestroyed);

        // Broken tools (1 durability left) can't mine or attack until repaired
        NeoForge.EVENT_BUS.addListener(BrokenToolHandler::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(BrokenToolHandler::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BrokenToolHandler::onBreakSpeed); // after MegaToolMining

        // Held-tool lighting: move the invisible light with the player, clean it up when needed
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onLogout);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onDeath);
        NeoForge.EVENT_BUS.addListener(ToolLightManager::onServerStopping);

        // Give back stored tools/plates if a tool is consumed by a non-carry-over crafting recipe
        NeoForge.EVENT_BUS.addListener(ToolCarryOver::onItemCrafted);
    }
}
