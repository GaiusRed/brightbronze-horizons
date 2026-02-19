package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Version-abstracted tool creation.
 * 
 * <p>Handles the major differences in tool creation between versions:
 * <ul>
 *   <li>1.21.10: Uses Item.Properties.sword(), .pickaxe(), etc. with ToolMaterial record</li>
 *   <li>1.21.1: Uses SwordItem, PickaxeItem classes with Tier interface</li>
 * </ul>
 */
public interface ToolFactory {
    
    /**
     * Creates a tool material/tier definition.
     * 
     * @param incorrectBlocksTag Tag of blocks this tier cannot mine
     * @param durability Tool durability
     * @param speed Mining speed
     * @param attackDamageBonus Base attack damage bonus
     * @param enchantability Enchantability value
     * @param repairItemsTag Tag of items that can repair this tool
     * @return An opaque handle to the tool material (version-specific type)
     */
    Object createToolMaterial(
            TagKey<Block> incorrectBlocksTag,
            int durability,
            float speed,
            float attackDamageBonus,
            int enchantability,
            TagKey<Item> repairItemsTag
    );
    
    /**
     * Creates a sword item.
     * 
     * @param key The item's resource key
     * @param material The tool material (from createToolMaterial)
     * @param attackDamage Attack damage bonus
     * @param attackSpeed Attack speed modifier
     * @param tab Creative tab
     * @return The sword item
     */
    Item createSword(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a pickaxe item.
     */
    Item createPickaxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates an axe item.
     */
    Item createAxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a shovel item.
     */
    Item createShovel(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab);
    
    /**
     * Creates a hoe item.
     */
    Item createHoe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab);
}
