package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.tool.MegaToolPoke;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * Registers every custom packet. Client -> server packets are re-validated on the server
 * (held item, tier, slot range) before acting. The one server -> client packet only drives
 * an animation.
 */
public final class ModNetworking
{
    private ModNetworking() {}

    /** Bump it if packet formats change: clients and servers with a different version can't connect. */
    private static final String PROTOCOL = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(MegaTools.id("main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    /** Called once from the mod constructor. */
    public static void register()
    {
        toServer(OpenToolGuiPayload.class, buf -> OpenToolGuiPayload.INSTANCE, ServerPayloadHandler::handleOpenGui);
        toServer(SwitchPlatePayload.class, buf -> SwitchPlatePayload.INSTANCE, ServerPayloadHandler::handleSwitchPlate);
        toServer(SwapToolPayload.class, SwapToolPayload::read, ServerPayloadHandler::handleSwapTool);
        toServer(RenameToolPayload.class, RenameToolPayload::read, ServerPayloadHandler::handleRename);
        toServer(MegaToolBreakPayload.class, buf -> MegaToolBreakPayload.INSTANCE, ServerPayloadHandler::handleMegaToolBreak);
        toServer(OpenSwapRulesPayload.class, buf -> OpenSwapRulesPayload.INSTANCE, ServerPayloadHandler::handleOpenSwapRules);
        toServer(SetSwapRulesPayload.class, SetSwapRulesPayload::read, ServerPayloadHandler::handleSetSwapRules);
        toServer(ApplyDefaultRulesPayload.class, ApplyDefaultRulesPayload::read, ServerPayloadHandler::handleApplyDefaultRules);
        toServer(ShaderLightPayload.class, ShaderLightPayload::read, ServerPayloadHandler::handleShaderLight);

        // Server -> client: another player's Mega Tool poke (third-person animation).
        // MegaToolPoke has no client-only imports, so this is safe to register on a dedicated server.
        toClient(MegaToolPokePayload.class, MegaToolPokePayload::read,
                (payload, context) -> context.enqueueWork(() -> MegaToolPoke.triggerRemote(payload.entityId())));
        // Server -> client: the server's tool stats, so the client can warn about a mismatch.
        toClient(ToolStatsPayload.class, ToolStatsPayload::read, ToolStatsPayload::handle);
    }

    private static <T extends Payload> void toServer(Class<T> type, Function<FriendlyByteBuf, T> reader, BiConsumer<T, PayloadContext> handler)
    {
        add(type, reader, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T extends Payload> void toClient(Class<T> type, Function<FriendlyByteBuf, T> reader, BiConsumer<T, PayloadContext> handler)
    {
        add(type, reader, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    // Handlers move their work to the main thread themselves (enqueueWork), like on NeoForge.
    private static <T extends Payload> void add(Class<T> type, Function<FriendlyByteBuf, T> reader, BiConsumer<T, PayloadContext> handler, NetworkDirection direction)
    {
        CHANNEL.messageBuilder(type, nextId++, direction)
                .encoder(Payload::write)
                .decoder(reader)
                .consumerNetworkThread((payload, context) ->
                {
                    handler.accept(payload, new PayloadContext(context.get()));
                    context.get().setPacketHandled(true);
                })
                .add();
    }
}
