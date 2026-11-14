package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.tool.StorageWhitelist;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server -> client on join and after /reload: the storage whitelist (config/megatools-storage-whitelist.json)
 * as the server loaded it, so the client's storage slots accept exactly what the server accepts.
 * Uses only common classes, so it is safe on servers.
 */
public record StorageWhitelistPayload(List<ResourceLocation> items, List<ResourceLocation> tags) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<StorageWhitelistPayload> TYPE =
            new CustomPacketPayload.Type<>(MegaTools.id("storage_whitelist"));
    public static final StreamCodec<ByteBuf, StorageWhitelistPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StorageWhitelistPayload::items,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StorageWhitelistPayload::tags,
            StorageWhitelistPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(StorageWhitelistPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> StorageWhitelist.apply(payload.items(), payload.tags()));
    }
}
