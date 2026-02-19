package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.block.ChunkSpawnerBlock;
import red.gaius.brightbronze.world.ChunkSpawnerTier;

/**
 * Registry for all mod blocks.
 * 
 * In MC 1.21.10+, blocks require their ID to be set on the properties BEFORE construction.
 * This is done via BlockBehaviour.Properties.setId(ResourceKey).
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.BLOCK);

    // ===== Helper to create ResourceKey =====
    
    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, 
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, name));
    }

    // ===== Brightbronze Material =====

    private static final ResourceLocation BRIGHTBRONZE_BLOCK_ID = 
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_block");
    
    public static final RegistrySupplier<Block> BRIGHTBRONZE_BLOCK = BLOCKS.register(
            BRIGHTBRONZE_BLOCK_ID,
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, BRIGHTBRONZE_BLOCK_ID))
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 15))); // Bright glow like glowstone

    // ===== Chunk Spawners =====
    
    /**
     * Creates standard properties for chunk spawner blocks.
     * All spawners share the same physical properties.
     */
    private static BlockBehaviour.Properties spawnerProperties(String name) {
        return BlockBehaviour.Properties.of()
                .setId(key(name))
                                // PRD: spawners should be easy to break (netherrack-like), and
                                // activation consumes the block and should always drop via loot table.
                                .strength(0.4f, 0.4f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 8); // Half as bright as glowstone/brightbronze block
    }

    public static final RegistrySupplier<Block> COAL_CHUNK_SPAWNER = BLOCKS.register(
            "coal_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("coal_chunk_spawner"), ChunkSpawnerTier.COAL));

    public static final RegistrySupplier<Block> COPPER_CHUNK_SPAWNER = BLOCKS.register(
            "copper_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("copper_chunk_spawner"), ChunkSpawnerTier.COPPER));

    public static final RegistrySupplier<Block> IRON_CHUNK_SPAWNER = BLOCKS.register(
            "iron_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("iron_chunk_spawner"), ChunkSpawnerTier.IRON));

    public static final RegistrySupplier<Block> GOLD_CHUNK_SPAWNER = BLOCKS.register(
            "gold_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("gold_chunk_spawner"), ChunkSpawnerTier.GOLD));

    public static final RegistrySupplier<Block> EMERALD_CHUNK_SPAWNER = BLOCKS.register(
            "emerald_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("emerald_chunk_spawner"), ChunkSpawnerTier.EMERALD));

    public static final RegistrySupplier<Block> DIAMOND_CHUNK_SPAWNER = BLOCKS.register(
            "diamond_chunk_spawner",
            () -> new ChunkSpawnerBlock(spawnerProperties("diamond_chunk_spawner"), ChunkSpawnerTier.DIAMOND));

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod blocks");
    }
}
