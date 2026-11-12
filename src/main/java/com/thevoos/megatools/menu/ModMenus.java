package com.thevoos.megatools.menu;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu (container) types. IMenuTypeExtension.create lets the server send extra data (the tool's hotbar slot). */
public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MegaTools.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ToolMenu>> TOOL_MENU = MENUS.register("tool_menu",
            () -> IMenuTypeExtension.create(ToolMenu::fromNetwork));

    private ModMenus() {}

    public static void register(IEventBus eventBus)
    {
        MENUS.register(eventBus);
    }
}
