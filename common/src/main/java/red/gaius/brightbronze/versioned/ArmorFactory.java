package red.gaius.brightbronze.versioned;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * Version-abstracted armor creation.
 * 
 * <p>Handles the major differences in armor creation between versions:
 * <ul>
 *   <li>1.21.10: Uses Item.Properties.humanoidArmor() with ArmorMaterial record</li>
 *   <li>1.21.1: Uses ArmorItem class with ArmorMaterial interface (registered)</li>
 * </ul>
 */
public interface ArmorFactory {
    
    /**
     * Armor slot types for cross-version compatibility.
     */
    enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }
    
    /**
     * Creates an armor material definition.
     * 
     * @param modId The mod ID for the equipment asset
     * @param name The material name (used for equipment asset path)
     * @param baseDurability Base durability multiplier
     * @param defense Map of slot to defense value
     * @param enchantability Enchantability value
     * @param equipSound Sound played when equipping (as Holder)
     * @param toughness Armor toughness
     * @param knockbackResistance Knockback resistance
     * @param repairItemsTag Tag of items that can repair this armor
     * @return An opaque handle to the armor material (version-specific type)
     */
    Object createArmorMaterial(
            String modId,
            String name,
            int baseDurability,
            Map<ArmorSlot, Integer> defense,
            int enchantability,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            TagKey<Item> repairItemsTag
    );
    
    /**
     * Creates an armor item for the given slot.
     * 
     * @param key The item's resource key
     * @param material The armor material (from createArmorMaterial)
     * @param slot The armor slot
     * @param baseDurability Base durability for calculating total durability
     * @param tab Creative tab
     * @return The armor item
     */
    Item createArmorItem(
            ResourceKey<Item> key,
            Object material,
            ArmorSlot slot,
            int baseDurability,
            ResourceKey<CreativeModeTab> tab
    );
    
    /**
     * Returns the durability for a given armor slot and base durability.
     * 
     * @param slot The armor slot
     * @param baseDurability The base durability multiplier
     * @return The actual durability value
     */
    int getDurability(ArmorSlot slot, int baseDurability);
    
    // ===== Convenience methods for creating individual armor pieces =====
    
    /**
     * Creates a helmet item.
     */
    default Item createHelmet(ResourceKey<Item> key, Object material, int baseDurability, ResourceKey<CreativeModeTab> tab) {
        return createArmorItem(key, material, ArmorSlot.HELMET, baseDurability, tab);
    }
    
    /**
     * Creates a chestplate item.
     */
    default Item createChestplate(ResourceKey<Item> key, Object material, int baseDurability, ResourceKey<CreativeModeTab> tab) {
        return createArmorItem(key, material, ArmorSlot.CHESTPLATE, baseDurability, tab);
    }
    
    /**
     * Creates leggings item.
     */
    default Item createLeggings(ResourceKey<Item> key, Object material, int baseDurability, ResourceKey<CreativeModeTab> tab) {
        return createArmorItem(key, material, ArmorSlot.LEGGINGS, baseDurability, tab);
    }
    
    /**
     * Creates boots item.
     */
    default Item createBoots(ResourceKey<Item> key, Object material, int baseDurability, ResourceKey<CreativeModeTab> tab) {
        return createArmorItem(key, material, ArmorSlot.BOOTS, baseDurability, tab);
    }
}
