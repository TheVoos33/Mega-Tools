package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;
import com.thevoos.megatools.component.SwapRules;

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
public record ApplyDefaultRulesPayload(SwapRules applied, SwapRules add) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        applied.write(buf);
        add.write(buf);
    }

    public static ApplyDefaultRulesPayload read(FriendlyByteBuf buf)
    {
        return new ApplyDefaultRulesPayload(SwapRules.read(buf, SwapRules.MAX_APPLIED), SwapRules.read(buf, SwapRules.MAX_TOTAL));
    }
}
