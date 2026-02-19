package red.gaius.brightbronze.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Custom tags for Brightbronze Horizons.
 */
public final class ModTags {

    public static final class Items {
        /**
         * Items that can repair Brightbronze tools (used in anvil).
         * Contains: brightbronze_ingot
         */
        public static final TagKey<Item> BRIGHTBRONZE_TOOL_MATERIALS = tag("brightbronze_tool_materials");

        /**
         * Items that can repair Brightbronze armor (used in anvil).
         * Contains: brightbronze_ingot
         */
        public static final TagKey<Item> REPAIRS_BRIGHTBRONZE_ARMOR = tag("repairs_brightbronze_armor");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, name));
        }

        private Items() {
        }
    }

    private ModTags() {
    }
}
