package red.gaius.brightbronze.world.mob1211;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import red.gaius.brightbronze.world.mob.MobSpawnTableManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * NeoForge 1.21.1-compatible reload listener for mob spawn tables.
 * 
 * <p>In MC 1.21.1, PreparableReloadListener uses the older signature with
 * PreparationBarrier, ResourceManager, and two ProfilerFillers. This differs
 * from 1.21.10+ which uses SharedState instead of ResourceManager.
 */
public final class MobSpawnTableReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier barrier,
        ResourceManager resourceManager,
        ProfilerFiller prepareProfiler,
        ProfilerFiller applyProfiler,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> MobSpawnTableManager.loadTables(resourceManager), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(MobSpawnTableManager::apply, gameExecutor);
    }
}
