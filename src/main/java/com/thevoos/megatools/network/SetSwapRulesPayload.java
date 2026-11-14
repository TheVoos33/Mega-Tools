package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.component.SwapRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: the Swap Rules GUI changed the held tool's rules (a filter was set, switched or
 * removed, or copied rules were pasted). Carries the complete new rule list; the server cleans it up
 * (limits, duplicates) and stores it on the held tool.
 */
public record SetSwapRulesPayload(SwapRules rules) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SetSwapRulesPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("set_swap_rules"));
    public static final StreamCodec<ByteBuf, SetSwapRulesPayload> STREAM_CODEC = StreamCodec.composite(
            SwapRules.STREAM_CODEC, SetSwapRulesPayload::rules,
            SetSwapRulesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
