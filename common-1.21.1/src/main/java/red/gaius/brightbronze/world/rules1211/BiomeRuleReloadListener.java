package red.gaius.brightbronze.world.rules1211;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * NeoForge 1.21.1-compatible reload listener for biome rules.
 * 
 * <p>In MC 1.21.1, PreparableReloadListener uses the older signature with
 * PreparationBarrier, ResourceManager, and two ProfilerFillers. This differs
 * from 1.21.10+ which uses SharedState instead of ResourceManager.
 */
public final class BiomeRuleReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier barrier,
        ResourceManager resourceManager,
        ProfilerFiller prepareProfiler,
        ProfilerFiller applyProfiler,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> BiomeRuleManager.loadRules(resourceManager), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(BiomeRuleManager::apply, gameExecutor);
    }
}
