package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.component.SwapRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: default rules of the player's config/megatools-default-rules.json for the held tool.
 *
 * The file is read by the client (every player tunes their own), but rules are saved on the tool by the
 * server, so the client sends the default rules a tool hasn't received yet:
 *
 * @param applied the default rules exactly as the file has them; remembered on the tool so each is only
 *                ever copied once (a default the player removed must not come back)
 * @param add     the same rules as filters to add to the tool (a tag or mod rule gets a block to show in
 *                its filter slot); empty when only the "already received" list is passed on (Paste)
 *
 * The server ignores the whole packet if the tool already knows every rule in {@code applied}.
 */
public record ApplyDefaultRulesPayload(SwapRules applied, SwapRules add) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ApplyDefaultRulesPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("apply_default_rules"));
    public static final StreamCodec<ByteBuf, ApplyDefaultRulesPayload> STREAM_CODEC = StreamCodec.composite(
            SwapRules.APPLIED_STREAM_CODEC, ApplyDefaultRulesPayload::applied,
            SwapRules.STREAM_CODEC, ApplyDefaultRulesPayload::add,
            ApplyDefaultRulesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
