package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for all mod block entities.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // ===== Chunk Spawner Block Entity (to be added in Phase 3) =====

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod block entities");
    }
}
