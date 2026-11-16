package com.thevoos.megatools.menu;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Locale;

/**
 * Which tool menu a player sees (config/megatools-common.toml: menuStyle). A choice of the player's own
 * game: both menus hold the same slots and buttons, only where they are drawn differs, so the server
 * never needs to know (see GuiLayout).
 */
public enum MenuStyle implements TranslatableEnum
{
    /** Not chosen yet: the next time the tool menu is opened, the player is asked (MenuStyleScreen). */
    ASK,
    /** Wide menu, every button with its name written out. */
    CLASSIC,
    /** Chest-sized menu, every button an item icon. */
    ICONS;

    @Override
    public Component getTranslatedName()
    {
        return Component.translatable("megatools.menu_style." + name().toLowerCase(Locale.ROOT));
    }
}
