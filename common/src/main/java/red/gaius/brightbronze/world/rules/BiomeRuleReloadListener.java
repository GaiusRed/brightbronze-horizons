package red.gaius.brightbronze.world.rules;

import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * NeoForge-friendly reload listener for biome rules.
 */
public final class BiomeRuleReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
        SharedState sharedState,
        Executor backgroundExecutor,
        PreparationBarrier barrier,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> BiomeRuleManager.loadRules(sharedState.resourceManager()), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(BiomeRuleManager::apply, gameExecutor);
    }
}
