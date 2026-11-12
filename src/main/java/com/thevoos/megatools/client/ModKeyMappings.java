package com.thevoos.megatools.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Three key bindings on the same physical key, separated by NeoForge key modifiers.
 * All three are rebindable under Options > Controls > Key Binds > "Mega Tools".
 */
public final class ModKeyMappings
{
    public static final String CATEGORY = "key.categories.megatools";

    /** Ctrl + R: open the tool GUI. */
    public static final KeyMapping OPEN_GUI = new KeyMapping("key.megatools.open_gui",
            KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Shift + R: load the other enchantment plate. */
    public static final KeyMapping SWITCH_PLATE = new KeyMapping("key.megatools.switch_plate",
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** R: swap to the pre-selected stored tool / swap back. */
    public static final KeyMapping SWAP_TOOL = new KeyMapping("key.megatools.swap_tool",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ModKeyMappings() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_GUI);
        event.register(SWITCH_PLATE);
        event.register(SWAP_TOOL);
    }

    /**
     * True if the binding's modifier is actually held right now. Guards against a plain "R"
     * press being counted for the Ctrl/Shift variants (and vice versa).
     */
    public static boolean modifierMatches(KeyMapping mapping)
    {
        KeyModifier modifier = mapping.getKeyModifier();
        if (modifier == KeyModifier.NONE)
            return !Screen.hasControlDown() && !Screen.hasShiftDown();
        return modifier.isActive(mapping.getKeyConflictContext());
    }
}
