package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for all mod block entities.
 * 
 * <p>Currently no block entities are registered. The ChunkSpawnerBlock
 * handles all its logic synchronously without needing a BlockEntity.
 * This registry is kept for future expansion (e.g., machines, containers).
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // ===== Future Block Entities =====
    // Add block entity registrations here when needed

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod block entities");
    }
}
