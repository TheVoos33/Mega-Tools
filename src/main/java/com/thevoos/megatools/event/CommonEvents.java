package com.thevoos.megatools.event;

import com.thevoos.megatools.command.MegaToolsCommand;
import com.thevoos.megatools.items.EnchantmentPlateItem;
import com.thevoos.megatools.recipe.ToolCarryOver;
import com.thevoos.megatools.tool.DisabledToolHandler;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.ToolLightManager;
import com.thevoos.megatools.tool.PlateHelper;
import com.thevoos.megatools.tool.ToolSwapper;
import com.thevoos.megatools.tool.TorchPickup;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;

/** Registers all gameplay listeners on the Forge (game) event bus. Runs on both sides. */
public final class CommonEvents
{
    private CommonEvents() {}

    public static void register()
    {
        // Mega Tool: hold delay, fresh-click bookkeeping (the instant break speed itself is set by PlayerMixin)
        MinecraftForge.EVENT_BUS.addListener(MegaToolMining::onBlockBroken);
        MinecraftForge.EVENT_BUS.addListener(MegaToolMining::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(MegaToolMining::onAttackEntity);   // third-person poke for others
        MinecraftForge.EVENT_BUS.addListener(MegaToolMining::onLogout);
        MinecraftForge.EVENT_BUS.addListener(MegaToolMining::onServerStopped);

        // Enchantment Plate + Enchanted Book in an anvil
        MinecraftForge.EVENT_BUS.addListener(PlateHelper::onAnvilUpdate);
        // Which enchantments plates accept is cached per enchantment; tags changed -> recompute
        MinecraftForge.EVENT_BUS.addListener(EnchantmentPlateItem::onTagsUpdated);

        // Return the Mega Tools tool if a swapped-out tool breaks
        MinecraftForge.EVENT_BUS.addListener(ToolSwapper::onItemDestroyed);

        // Broken tools (1 durability left) and tools with Break / Attack switched off can't mine / attack
        MinecraftForge.EVENT_BUS.addListener(DisabledToolHandler::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DisabledToolHandler::onAttackEntity); // before the Mega Tool's poke
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DisabledToolHandler::onBlockBroken); // before MegaToolMining uses up the granted break

        // Held-tool lighting: move the invisible light with the player, clean it up when needed
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onServerTick);      // fade out replaced lights
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onLogout);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onChangeDimension);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onDeath);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onChunkSent);       // re-send light to clients still loading chunks
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onChunkUnwatch);
        MinecraftForge.EVENT_BUS.addListener(ToolLightManager::onBlockPlaced);     // lights in a portal frame would stop it from lighting

        // Picked-up torches go into the torch slot of a paxel or Mega Tool that already holds them
        MinecraftForge.EVENT_BUS.addListener(TorchPickup::onItemPickup);

        // Give back stored tools/plates if a tool is consumed by a non-carry-over crafting recipe
        MinecraftForge.EVENT_BUS.addListener(ToolCarryOver::onItemCrafted);

        // Tell joining players the server's tool stats (startup config isn't synced)
        MinecraftForge.EVENT_BUS.addListener(com.thevoos.megatools.network.ToolStatsPayload::onPlayerLoggedIn);

        // /megatools commands (kit)
        MinecraftForge.EVENT_BUS.addListener(MegaToolsCommand::onRegisterCommands);
    }
}
