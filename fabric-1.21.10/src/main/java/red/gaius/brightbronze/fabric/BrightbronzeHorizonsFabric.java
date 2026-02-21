package red.gaius.brightbronze.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhAdminCommands;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.command.BbhDiskCommands;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.versioned.mc12110.McVersion12110;
import red.gaius.brightbronze.world.mob.MobSpawnTableManager;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;

/**
 * Fabric entrypoint for Brightbronze Horizons.
 */
public final class BrightbronzeHorizonsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Initialize version-specific implementation (MC 1.21.10)
        Versioned.init(new McVersion12110());
        
        // Run common setup
        BrightbronzeHorizons.init();
        
        // Initialize networking (safe to call here on Fabric)
        BrightbronzeHorizons.initNetworking();

        // Phase 7: datapack-driven mob spawn tables
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "mob_spawns");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                MobSpawnTableManager.reload(manager);
            }
        });

        // Phase 8/9: data-driven biome rules (tier mapping, weights, post-processing, per-biome mob spawns)
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "biome_rules");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                BiomeRuleManager.reload(manager);
            }
        });

        // Debug commands (helps inspect dynamic source dimensions).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BbhDebugCommands.register(dispatcher)
        );

        // Phase 11: disk management and usage reporting.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BbhDiskCommands.register(dispatcher)
        );

        // Phase 12: admin commands.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BbhAdminCommands.register(dispatcher)
        );
    }
}
