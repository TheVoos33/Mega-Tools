package com.thevoos.megatools.network;

import com.thevoos.megatools.tool.MegaToolPoke;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every custom packet. Client -> server packets are re-validated on the server
 * (held item, tier, slot range) before acting. The one server -> client packet only drives
 * an animation.
 */
public final class ModNetworking
{
    private ModNetworking() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        // "1" = protocol version; bump it if packet formats change.
        PayloadRegistrar registrar = event.registrar("5");

        registrar.playToServer(OpenToolGuiPayload.TYPE, OpenToolGuiPayload.STREAM_CODEC, ServerPayloadHandler::handleOpenGui);
        registrar.playToServer(SwitchPlatePayload.TYPE, SwitchPlatePayload.STREAM_CODEC, ServerPayloadHandler::handleSwitchPlate);
        registrar.playToServer(SwapToolPayload.TYPE, SwapToolPayload.STREAM_CODEC, ServerPayloadHandler::handleSwapTool);
        registrar.playToServer(RenameToolPayload.TYPE, RenameToolPayload.STREAM_CODEC, ServerPayloadHandler::handleRename);
        registrar.playToServer(MegaToolBreakPayload.TYPE, MegaToolBreakPayload.STREAM_CODEC, ServerPayloadHandler::handleMegaToolBreak);
        registrar.playToServer(OpenSwapRulesPayload.TYPE, OpenSwapRulesPayload.STREAM_CODEC, ServerPayloadHandler::handleOpenSwapRules);
        registrar.playToServer(SetSwapRulesPayload.TYPE, SetSwapRulesPayload.STREAM_CODEC, ServerPayloadHandler::handleSetSwapRules);
        registrar.playToServer(ApplyDefaultRulesPayload.TYPE, ApplyDefaultRulesPayload.STREAM_CODEC, ServerPayloadHandler::handleApplyDefaultRules);
        registrar.playToServer(ShaderLightPayload.TYPE, ShaderLightPayload.STREAM_CODEC, ServerPayloadHandler::handleShaderLight);
        registrar.playToServer(ToggleAbilityPayload.TYPE, ToggleAbilityPayload.STREAM_CODEC, ServerPayloadHandler::handleToggleAbility);

        // Server -> client: another player's Mega Tool poke (third-person animation).
        // MegaToolPoke has no client-only imports, so this is safe to register on a dedicated server.
        registrar.playToClient(MegaToolPokePayload.TYPE, MegaToolPokePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MegaToolPoke.triggerRemote(payload.entityId())));
        // Server -> client: the server's tool stats, so the client can warn about a mismatch.
        registrar.playToClient(ToolStatsPayload.TYPE, ToolStatsPayload.STREAM_CODEC, ToolStatsPayload::handle);
    }
}
