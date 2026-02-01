package red.gaius.brightbronze.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.command.BbhDebugCommands;
import red.gaius.brightbronze.world.mob.MobSpawnTableManager;

/**
 * Fabric entrypoint for Brightbronze Horizons.
 */
public final class BrightbronzeHorizonsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run common setup
        BrightbronzeHorizons.init();

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

        // Debug commands (helps inspect dynamic source dimensions).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BbhDebugCommands.register(dispatcher)
        );
    }
}
