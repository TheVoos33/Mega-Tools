package com.thevoos.megatools.client;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.tool.LightSources;
import com.thevoos.megatools.tool.ToolLightManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Dynamic lights mods (Sodium Dynamic Lights, LambDynamicLights, RyoamicLights) light the area around
 * entities on the client, smoothly and without placing blocks. When one of them is installed, a held
 * tool with torches is lit by that mod instead of by Mega Tools' invisible light blocks:
 *
 *  - here, players holding a lit tool are registered with the mod as a light source of the tool's
 *    brightness (those mods only know which ITEMS glow, and a tool only glows with torches in it and
 *    its light switched on, so the answer has to come from us);
 *  - ShaderLightClient then tells the server to stop placing light blocks for this player, the same
 *    way it does while a shader pack lights the held tool.
 *
 * All three mods share one API shape, DynamicLightHandlers.registerDynamicLightHandler(EntityType,
 * DynamicLightHandler), in their own packages. It is used by reflection with a proxy for the handler
 * interface, so none of them is needed to build or run Mega Tools. If a mod is installed but could not
 * be hooked (its API changed), the light blocks stay on: the tool must never end up without light.
 */
public final class DynamicLightsCompat
{
    /** mod id, package of its DynamicLightHandlers / DynamicLightHandler */
    private static final String[][] MODS = {
            {"sodiumdynamiclights", "dev.lambdaurora.lambdynlights.api"},
            {"lambdynlights", "dev.lambdaurora.lambdynlights.api"},
            {"ryoamiclights", "org.thinkingstudio.ryoamiclights.api"},
    };

    private static boolean active;

    private DynamicLightsCompat() {}

    /** True if a dynamic lights mod lights the held tool, so the server shouldn't place light blocks for this player. */
    public static boolean isActive()
    {
        return active;
    }

    /** Called once while the client starts. */
    public static void init()
    {
        for (String[] mod : MODS)
        {
            if (!ModList.get().isLoaded(mod[0]))
                continue;
            try
            {
                register(mod[1]);
                active = true;
                MegaTools.LOGGER.info("{} found: it lights held tools, Mega Tools places no light blocks for this player", mod[0]);
            }
            catch (ReflectiveOperationException | LinkageError | RuntimeException e)
            {
                MegaTools.LOGGER.warn("{} is installed but its light API could not be used; keeping Mega Tools' own held light", mod[0], e);
            }
        }
    }

    private static void register(String apiPackage) throws ReflectiveOperationException
    {
        Class<?> handlers = Class.forName(apiPackage + ".DynamicLightHandlers");
        Class<?> handler = Class.forName(apiPackage + ".DynamicLightHandler");

        InvocationHandler logic = (proxy, method, args) -> switch (method.getName())
        {
            // int getLuminance(T entity): the tool's light level while this player holds a lit tool
            case "getLuminance" -> args != null && args.length == 1 && args[0] instanceof Player player
                    && LightSources.isHoldingLight(player) ? ToolLightManager.LIGHT_LEVEL : 0;
            case "isWaterSensitive" -> false; // torches inside a tool don't go out under water
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "MegaToolsHeldLight";
            // Methods a newer version of the mod adds: their own default code if they have one, else a neutral
            // value. Throwing here would crash the game while it draws a frame.
            default -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : neutral(method.getReturnType());
        };
        Object playerLight = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[]{handler}, logic);
        // Existing handlers for players are kept: the mods combine them and use the brightest.
        handlers.getMethod("registerDynamicLightHandler", EntityType.class, handler).invoke(null, EntityType.PLAYER, playerLight);
    }

    /** false / 0 / null for a method the handler doesn't know. */
    private static Object neutral(Class<?> type)
    {
        if (type == boolean.class)
            return false;
        if (type == char.class)
            return (char) 0;
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0f;
        if (type == double.class)
            return 0d;
        return null; // void and objects
    }
}
