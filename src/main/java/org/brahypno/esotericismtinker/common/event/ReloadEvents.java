package org.brahypno.esotericismtinker.common.event;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.brahypno.esotericismtinker.library.recipe.selenic.SelenicRecipeCache;
import org.brahypno.esotericismtinker.utils.PartInfoLookup;

import java.util.concurrent.CompletableFuture;

public final class ReloadEvents {
    private ReloadEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((
                                  barrier, manager, preparationsProfiler, reloadProfiler,
                                  backgroundExecutor, gameExecutor) ->
                                  CompletableFuture
                                          .runAsync(ReloadEvents::clearCaches, gameExecutor)
                                          .thenCompose(barrier::wait)
        );
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        clearCaches();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearCaches();
    }

    private static void clearCaches() {
        SelenicRecipeCache.clear();
        PartInfoLookup.clearRuntime();
    }
}
