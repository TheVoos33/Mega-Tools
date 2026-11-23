package com.thevoos.megatools.items;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** The "Mega Tools" creative tab (same as your original, plus the Enchantment Plate). */
public class ModCreativeModeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MegaTools.MODID);

    public static final Supplier<CreativeModeTab> MEGA_TOOLS_TAB = CREATIVE_MODE_TAB.register("mega_tools",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MEGA_TOOL.get()))
                    .title(Component.translatable("creativetab.megatools.megatools_items"))
                    .displayItems((itemDisplayParameters, output) ->
                    {
                        // Tools
                        output.accept(ModItems.STONE_PAXEL);
                        output.accept(ModItems.IRON_PAXEL);
                        output.accept(ModItems.DIAMOND_PAXEL);
                        output.accept(ModItems.NETHERITE_PAXEL);
                        ModItems.megaTools().forEach(output::accept); // + Allthemodium upgrades when installed
                        // Ingredients
                        output.accept(ModItems.IRON_HEAD);
                        output.accept(ModItems.DIAMOND_HEAD);
                        output.accept(ModItems.NETHERITE_HEAD);
                        output.accept(ModItems.MEGA_TOOL_CORE);
                        // Upgrade
                        output.accept(ModItems.ENCHANTMENT_PLATE);
                        output.accept(ModItems.SUPPORT_TOOL);
                    }).build());

    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
