package red.gaius.brightbronze.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.rules.BlockReplacementRule;

import java.util.List;

/**
 * Applies post-processing rules to spawned chunks (e.g., stripping/replacing blocks).
 */
public final class ChunkPostProcessor {

    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");

    private ChunkPostProcessor() {
    }

    public static int apply(ServerLevel level, ChunkPos chunkPos, List<BlockReplacementRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return 0;
        }

        int replaced = 0;
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    BlockReplacementRule match = firstMatch(state, rules);
                    if (match == null) {
                        continue;
                    }

                    Block replacement = resolveReplacement(match.replacementBlockId());
                    if (replacement == null) {
                        continue;
                    }

                    BlockState replacementState = replacement.defaultBlockState();
                    if (replacementState == state) {
                        continue;
                    }

                    level.setBlock(pos, replacementState, Block.UPDATE_ALL);
                    replaced++;
                }
            }
        }

        if (replaced > 0) {
            BrightbronzeHorizons.LOGGER.debug("Post-processed chunk ({}, {}) with {} replacements", chunkPos.x, chunkPos.z, replaced);
        }

        return replaced;
    }

    private static BlockReplacementRule firstMatch(BlockState state, List<BlockReplacementRule> rules) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        for (BlockReplacementRule rule : rules) {
            if (rule.isTagMatch()) {
                if (state.is(rule.matchTag())) {
                    return rule;
                }
            } else if (rule.matchBlockId() != null && rule.matchBlockId().equals(id)) {
                return rule;
            }
        }

        return null;
    }

    private static Block resolveReplacement(ResourceLocation replacementId) {
        if (replacementId == null) {
            return null;
        }
        if (replacementId.equals(AIR_ID)) {
            return Blocks.AIR;
        }

        var refOpt = BuiltInRegistries.BLOCK.get(replacementId);
        return refOpt.isEmpty() ? null : refOpt.get().value();
    }
}
