package com.thevoos.megatools.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thevoos.megatools.KeyNames;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Four key bindings on the same physical key, separated by Forge key modifiers.
 * All four are rebindable under Options > Controls > Key Binds > "Mega Tools".
 */
public final class ModKeyMappings
{
    public static final String CATEGORY = "key.categories.megatools";

    /** Ctrl + R: open the tool GUI. */
    public static final KeyMapping OPEN_GUI = new KeyMapping(KeyNames.OPEN_GUI,
            KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Alt + R: load the other enchantment plate. */
    public static final KeyMapping SWITCH_PLATE = new KeyMapping(KeyNames.SWITCH_PLATE,
            KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** R: swap to the pre-selected stored tool / swap back. */
    public static final KeyMapping SWAP_TOOL = new KeyMapping(KeyNames.SWAP_TOOL,
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Shift + R: swap to the last stored tool swapped to, whatever block is targeted. */
    public static final KeyMapping SWAP_LAST = new KeyMapping(KeyNames.SWAP_LAST,
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ModKeyMappings() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_GUI);
        event.register(SWITCH_PLATE);
        event.register(SWAP_TOOL);
        event.register(SWAP_LAST);
    }

    /**
     * True if the binding's modifier is actually held right now. Guards against a plain "R"
     * press being counted for the Ctrl/Shift/Alt variants (and vice versa).
     */
    public static boolean modifierMatches(KeyMapping mapping)
    {
        KeyModifier modifier = mapping.getKeyModifier();
        if (modifier == KeyModifier.NONE)
            return !Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown();
        return modifier.isActive(mapping.getKeyConflictContext());
    }
}
