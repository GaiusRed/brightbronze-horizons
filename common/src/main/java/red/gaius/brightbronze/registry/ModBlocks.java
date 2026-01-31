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

/**
 * Registry for all mod blocks.
 * 
 * In MC 1.21.10+, blocks require their ID to be set on the properties BEFORE construction.
 * This is done via BlockBehaviour.Properties.setId(ResourceKey).
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BrightbronzeHorizons.MOD_ID, Registries.BLOCK);

    // ===== Brightbronze Material =====

    private static final ResourceLocation BRIGHTBRONZE_BLOCK_ID = 
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_block");
    
    public static final RegistrySupplier<Block> BRIGHTBRONZE_BLOCK = BLOCKS.register(
            BRIGHTBRONZE_BLOCK_ID,
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, BRIGHTBRONZE_BLOCK_ID))
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // ===== Chunk Spawners (to be added in Phase 3) =====

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
        BrightbronzeHorizons.LOGGER.debug("Registered mod blocks");
    }
}
