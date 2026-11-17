package com.thevoos.megatools.menu;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Menu (container) types. IForgeMenuType.create lets the server send extra data (the tool's hotbar slot). */
public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MegaTools.MODID);

    public static final RegistryObject<MenuType<ToolMenu>> TOOL_MENU = MENUS.register("tool_menu",
            () -> IForgeMenuType.create(ToolMenu::fromNetwork));

    public static final RegistryObject<MenuType<SwapRulesMenu>> SWAP_RULES_MENU = MENUS.register("swap_rules_menu",
            () -> IForgeMenuType.create(SwapRulesMenu::fromNetwork));

    private ModMenus() {}

    public static void register(IEventBus eventBus)
    {
        MENUS.register(eventBus);
    }
}
