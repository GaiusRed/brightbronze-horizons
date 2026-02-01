package red.gaius.brightbronze.world.rules;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

/**
 * Defines a single block replacement rule applied to a spawned chunk.
 *
 * <p>Match can be either a block id or a block tag. Replacement is always a block id.
 * Replacement to {@code minecraft:air} represents removal.
 */
public final class BlockReplacementRule {

    private final TagKey<Block> matchTag;
    private final ResourceLocation matchBlockId;
    private final ResourceLocation replacementBlockId;

    private BlockReplacementRule(TagKey<Block> matchTag, ResourceLocation matchBlockId, ResourceLocation replacementBlockId) {
        this.matchTag = matchTag;
        this.matchBlockId = matchBlockId;
        this.replacementBlockId = Objects.requireNonNull(replacementBlockId, "replacementBlockId");
    }

    public static BlockReplacementRule matchTag(ResourceLocation tagId, ResourceLocation replacementBlockId) {
        return new BlockReplacementRule(TagKey.create(Registries.BLOCK, tagId), null, replacementBlockId);
    }

    public static BlockReplacementRule matchBlock(ResourceLocation blockId, ResourceLocation replacementBlockId) {
        return new BlockReplacementRule(null, blockId, replacementBlockId);
    }

    public boolean isTagMatch() {
        return matchTag != null;
    }

    public TagKey<Block> matchTag() {
        return matchTag;
    }

    public ResourceLocation matchBlockId() {
        return matchBlockId;
    }

    public ResourceLocation replacementBlockId() {
        return replacementBlockId;
    }
}
