package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import red.gaius.brightbronze.BrightbronzeHorizons;

import java.util.Map;

/**
 * Registry for armor materials and armor items.
 * 
 * In MC 1.21.10+, armor is created via Item.Properties#humanoidArmor() method
 * instead of the old ArmorItem class.
 */
public final class ModArmorMaterials {

    /**
     * Base durability multiplier for Brightbronze armor.
     * Positioned between Iron (15) and Diamond (33).
     */
    public static final int BASE_DURABILITY = 18;

    /**
     * Equipment asset key for armor rendering.
     * This determines which texture files are used for the armor model.
     */
    public static final ResourceKey<EquipmentAsset> BRIGHTBRONZE_EQUIPMENT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze"));

    /**
     * Brightbronze armor material - positioned between Iron and Diamond.
     * Defense values: Boots=2, Leggings=5, Chestplate=6, Helmet=2 (Iron: 2,5,6,2; Diamond: 3,6,8,3)
     * Enchantability: 18 (Iron: 9, Diamond: 10)
     * Toughness: 1.0f (Iron: 0, Diamond: 2.0f)
     * Knockback Resistance: 0 (same as Iron/Diamond)
     */
    public static final ArmorMaterial BRIGHTBRONZE = new ArmorMaterial(
            BASE_DURABILITY,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2
            ),
            18, // enchantability
            SoundEvents.ARMOR_EQUIP_IRON,
            1.0f, // toughness
            0.0f, // knockback resistance
            ItemTags.REPAIRS_IRON_ARMOR, // repair items tag
            BRIGHTBRONZE_EQUIPMENT_ASSET
    );

    // ===== Brightbronze Armor Items =====
    // In 1.21.10+, armor items are created with Item::new and humanoidArmor() property

    public static final RegistrySupplier<Item> BRIGHTBRONZE_HELMET = ModItems.ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_helmet"),
            () -> new Item(new Item.Properties()
                    .humanoidArmor(BRIGHTBRONZE, ArmorType.HELMET)
                    .durability(ArmorType.HELMET.getDurability(BASE_DURABILITY))));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_CHESTPLATE = ModItems.ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_chestplate"),
            () -> new Item(new Item.Properties()
                    .humanoidArmor(BRIGHTBRONZE, ArmorType.CHESTPLATE)
                    .durability(ArmorType.CHESTPLATE.getDurability(BASE_DURABILITY))));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_LEGGINGS = ModItems.ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_leggings"),
            () -> new Item(new Item.Properties()
                    .humanoidArmor(BRIGHTBRONZE, ArmorType.LEGGINGS)
                    .durability(ArmorType.LEGGINGS.getDurability(BASE_DURABILITY))));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_BOOTS = ModItems.ITEMS.register(
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "brightbronze_boots"),
            () -> new Item(new Item.Properties()
                    .humanoidArmor(BRIGHTBRONZE, ArmorType.BOOTS)
                    .durability(ArmorType.BOOTS.getDurability(BASE_DURABILITY))));

    private ModArmorMaterials() {
    }

    public static void register() {
        // ArmorMaterial is no longer registry-based in 1.21.10, it's created directly
        // The armor items are registered via ModItems.ITEMS
        BrightbronzeHorizons.LOGGER.debug("Initialized mod armor materials");
    }
}
