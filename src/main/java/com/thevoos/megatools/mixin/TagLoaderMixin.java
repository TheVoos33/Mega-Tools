package com.thevoos.megatools.mixin;

import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.tool.StorageWhitelist;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds the entries of config/megatools-storage-whitelist.json to the item tag #megatools:storable_item
 * while item tags are loaded (world load and /reload). They are real tag entries: synced to clients and
 * visible to JEI/EMI and other mods like any other.
 *
 * They are put IN FRONT of the entries from datapacks. A tag is built by going through its entries in
 * order, adding and removing, so a datapack's "remove" list comes after them and takes them out again:
 * a datapack (or KubeJS) has the last word over the config file.
 */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin
{
    @Shadow @Final private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void megatools$addStorableItems(ResourceManager resourceManager,
            CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir)
    {
        if (!Registries.tagsDirPath(Registries.ITEM).equals(this.directory))
            return;

        List<TagLoader.EntryWithSource> tag =
                cir.getReturnValue().computeIfAbsent(ModTags.STORABLE_ITEM.location(), id -> new ArrayList<>());
        List<TagLoader.EntryWithSource> fromConfig = new ArrayList<>();
        for (TagEntry entry : StorageWhitelist.loadTagEntries())
            fromConfig.add(new TagLoader.EntryWithSource(entry, StorageWhitelist.FILE_NAME));
        tag.addAll(0, fromConfig);
    }
}
