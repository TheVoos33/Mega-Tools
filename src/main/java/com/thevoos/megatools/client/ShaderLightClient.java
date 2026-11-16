package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.network.ShaderLightPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Method;

/**
 * Tells the server whether something on this client lights the held tool by itself, so it can stop
 * placing light blocks for this player: an active shader pack (Iris / Oculus), whose handheld light
 * does the job (see MultiToolItem#getLightEmission), or a dynamic lights mod (see DynamicLightsCompat).
 *
 * Iris is optional, so its API (IrisApi.getInstance().isShaderPackInUse()) is called by reflection:
 * without Iris this is simply always "no shader". Checked once a second; sent only when it changes.
 */
public final class ShaderLightClient
{
    private static final int CHECK_TICKS = 20;

    private static boolean irisLookedUp;
    private static Object irisApi;
    private static Method isShaderPackInUse;

    private static Boolean lastSent; // null = nothing sent on this connection yet
    private static int cooldown;

    private ShaderLightClient() {}

    /** New connection: send the current state again. */
    public static void reset()
    {
        lastSent = null;
        cooldown = 0;
    }

    /** Client tick. */
    public static void tick(Minecraft mc)
    {
        if (mc.player == null || mc.getConnection() == null || --cooldown > 0)
            return;
        cooldown = CHECK_TICKS;

        boolean active = DynamicLightsCompat.isActive() || isShaderPackActive();
        if ((lastSent == null || lastSent != active) && mc.getConnection().hasChannel(ShaderLightPayload.TYPE))
        {
            lastSent = active;
            PacketDistributor.sendToServer(new ShaderLightPayload(active));
        }
    }

    private static boolean isShaderPackActive()
    {
        if (!irisLookedUp)
        {
            irisLookedUp = true;
            try
            {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                isShaderPackInUse = api.getMethod("isShaderPackInUse");
            }
            catch (ClassNotFoundException e)
            {
                irisApi = null; // Iris isn't installed
            }
            catch (Throwable e)
            {
                irisApi = null;
                MegaTools.LOGGER.warn("Couldn't access the Iris API; shader handheld light detection is off", e);
            }
        }
        if (irisApi == null)
            return false;
        try
        {
            return (Boolean) isShaderPackInUse.invoke(irisApi);
        }
        catch (Throwable e)
        {
            irisApi = null;
            return false;
        }
    }
}
