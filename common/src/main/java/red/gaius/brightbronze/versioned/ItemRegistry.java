package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Version-abstracted item property and registration helpers.
 * 
 * <p>Handles the differences in Item.Properties between Minecraft versions,
 * particularly the setId() requirement in 1.21.10+.
 */
public interface ItemRegistry {
    
    /**
     * Creates a new Item.Properties with the given ID configured.
     * 
     * <p>In 1.21.10: calls {@code new Item.Properties().setId(key)}
     * <p>In 1.21.1: returns {@code new Item.Properties()} (no setId)
     * 
     * @param itemKey The resource key for the item
     * @return Configured Item.Properties
     */
    Item.Properties properties(ResourceKey<Item> itemKey);
    
    /**
     * Creates Item.Properties for a simple item with a creative tab.
     * 
     * @param itemKey The resource key for the item
     * @param tab The creative mode tab to add the item to
     * @return Configured Item.Properties with tab
     */
    Item.Properties properties(ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a simple Item with the given ID and creative tab.
     * 
     * @param itemKey The resource key for the item
     * @param tab The creative mode tab
     * @return A new Item instance
     */
    Item simpleItem(ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a BlockItem for the given block.
     * 
     * @param block The block to create an item for
     * @param itemKey The resource key for the item
     * @param tab The creative mode tab
     * @return A new BlockItem instance
     */
    Item blockItem(Block block, ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a ResourceKey for an item in this mod's namespace.
     * 
     * @param name The item name
     * @return The resource key
     */
    ResourceKey<Item> key(String modId, String name);
}
