package com.thevoos.megatools.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

import java.util.function.Consumer;

/**
 * A SimpleContainer that reports every change to one listener. Minecraft 26.1 removed the listeners
 * of SimpleContainer, which the tool menu uses to write each change straight back into the tool.
 */
public class ListenedContainer extends SimpleContainer
{
    private Consumer<Container> listener = container -> {};

    public ListenedContainer(int size)
    {
        super(size);
    }

    /** Set after the container is filled, so loading the tool's data doesn't trigger a save. */
    public void setListener(Consumer<Container> listener)
    {
        this.listener = listener;
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        listener.accept(this);
    }
}
