package com.thevoos.megatools;

/**
 * Names of the mod's key bindings (also their translation keys). Kept in common code so items can show
 * the CURRENT binding in tooltips with Component.keybind(...): the client resolves it every time the text
 * is drawn, including NeoForge modifiers ("Ctrl + R"), so rebinding in Controls updates every tooltip.
 * The KeyMappings themselves live in the client-only ModKeyMappings.
 */
public final class KeyNames
{
    public static final String OPEN_GUI = "key.megatools.open_gui";
    public static final String SWITCH_PLATE = "key.megatools.switch_plate";
    public static final String SWAP_TOOL = "key.megatools.swap_tool";
    public static final String SWAP_LAST = "key.megatools.swap_last";
    /** + AbilityToggles.NAMES[i]: one key binding per ability toggle, e.g. key.megatools.toggle.strip. */
    public static final String TOGGLE_PREFIX = "key.megatools.toggle.";

    private KeyNames() {}
}
