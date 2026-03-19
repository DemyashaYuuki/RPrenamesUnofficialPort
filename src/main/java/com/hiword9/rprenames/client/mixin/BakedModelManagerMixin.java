package com.hiword9.rprenames.client.mixin;

import java.util.concurrent.CompletableFuture;

import com.hiword9.rprenames.client.RPRenamesClient;
import com.hiword9.rprenames.client.model.RenameCatalog;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.item.ItemAssetsLoader;
import net.minecraft.client.render.model.BakedModelManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BakedModelManager.class)
public abstract class BakedModelManagerMixin {
    @Inject(method = "reload", at = @At("RETURN"))
    private static void rprenames$afterReload(
            CallbackInfoReturnable<CompletableFuture<Void>> cir,
            @Local(ordinal = 4) CompletableFuture<ItemAssetsLoader.Result> itemAssetsLoaderResult
    ) {
        try {
            RenameCatalog.rebuild(itemAssetsLoaderResult.join().contents());
            RPRenamesClient.LOGGER.info("RP Renames catalog rebuilt for {} items", RenameCatalog.size());
        } catch (Exception exception) {
            RPRenamesClient.LOGGER.error("Failed to rebuild rename catalog", exception);
        }
    }
}
