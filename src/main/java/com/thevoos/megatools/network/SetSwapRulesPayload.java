package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;
import com.thevoos.megatools.component.SwapRules;

/**
 * Client -> server: the Swap Rules GUI changed the held tool's rules (a filter was set, switched or
 * removed, or copied rules were pasted). Carries the complete new rule list; the server cleans it up
 * (limits, duplicates) and stores it on the held tool.
 */
public record SetSwapRulesPayload(SwapRules rules) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        rules.write(buf);
    }

    public static SetSwapRulesPayload read(FriendlyByteBuf buf)
    {
        return new SetSwapRulesPayload(SwapRules.read(buf, SwapRules.MAX_TOTAL));
    }
}
