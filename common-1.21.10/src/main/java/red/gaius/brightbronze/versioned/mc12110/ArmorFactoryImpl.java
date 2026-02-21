package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import red.gaius.brightbronze.versioned.ArmorFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Minecraft 1.21.10 implementation of ArmorFactory.
 * 
 * <p>In 1.21.10, armor is created via Item.Properties.humanoidArmor() and
 * ArmorMaterial is a record class with EquipmentAsset support.
 */
public class ArmorFactoryImpl implements ArmorFactory {
    
    @Override
    public Object createArmorMaterial(
            String modId,
            String name,
            int baseDurability,
            Map<ArmorSlot, Integer> defense,
            int enchantability,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            TagKey<Item> repairItemsTag) {
        
        // Create the equipment asset key for armor rendering
        ResourceKey<EquipmentAsset> equipmentAsset = ResourceKey.create(
                EquipmentAssets.ROOT_ID,
                ResourceLocation.fromNamespaceAndPath(modId, name));
        
        // Convert our ArmorSlot to MC's ArmorType
        Map<ArmorType, Integer> defenseMap = new EnumMap<>(ArmorType.class);
        defense.forEach((slot, value) -> defenseMap.put(toArmorType(slot), value));
        
        return new ArmorMaterial(
                baseDurability,
                defenseMap,
                enchantability,
                equipSound,
                toughness,
                knockbackResistance,
                repairItemsTag,
                equipmentAsset
        );
    }
    
    @Override
    public Item createArmorItem(
            ResourceKey<Item> key,
            Object material,
            ArmorSlot slot,
            int baseDurability,
            ResourceKey<CreativeModeTab> tab) {
        
        ArmorMaterial armorMaterial = (ArmorMaterial) material;
        ArmorType armorType = toArmorType(slot);
        
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .humanoidArmor(armorMaterial, armorType)
                .durability(armorType.getDurability(baseDurability)));
    }
    
    @Override
    public int getDurability(ArmorSlot slot, int baseDurability) {
        return toArmorType(slot).getDurability(baseDurability);
    }
    
    private static ArmorType toArmorType(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> ArmorType.HELMET;
            case CHESTPLATE -> ArmorType.CHESTPLATE;
            case LEGGINGS -> ArmorType.LEGGINGS;
            case BOOTS -> ArmorType.BOOTS;
        };
    }
}
