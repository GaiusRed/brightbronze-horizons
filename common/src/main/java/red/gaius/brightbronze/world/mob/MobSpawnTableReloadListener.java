package red.gaius.brightbronze.world.mob;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * NeoForge-friendly reload listener for mob spawn tables.
 */
public final class MobSpawnTableReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
        SharedState sharedState,
        Executor backgroundExecutor,
        PreparationBarrier barrier,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> MobSpawnTableManager.loadTables(sharedState.resourceManager()), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(MobSpawnTableManager::apply, gameExecutor);
    }
}
