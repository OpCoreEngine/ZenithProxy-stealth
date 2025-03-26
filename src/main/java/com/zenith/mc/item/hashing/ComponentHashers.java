/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package com.zenith.mc.item.hashing;

import com.google.common.hash.HashCode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("UnstableApiUsage")
public class ComponentHashers {
    private static final Map<DataComponentType<?>, MinecraftHasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.CUSTOM_DATA, MinecraftHasher.MNBT);
        registerInt(DataComponentTypes.MAX_STACK_SIZE);
        registerInt(DataComponentTypes.MAX_DAMAGE);
        registerInt(DataComponentTypes.DAMAGE);
        registerUnit(DataComponentTypes.UNBREAKABLE);

        // TODO custom name, component
        // TODO item name, component

        register(DataComponentTypes.ITEM_MODEL, MinecraftHasher.KEY);

        // TODO lore, component

        register(DataComponentTypes.RARITY, MinecraftHasher.RARITY);
        register(DataComponentTypes.ENCHANTMENTS, MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).convert(ItemEnchantments::getEnchantments));

        // TODO can place on/can break on, complicated
        // TODO attribute modifiers, attribute registry and equipment slot group hashers

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalList("floats", MinecraftHasher.FLOAT, CustomModelData::floats)
            .optionalList("flags", MinecraftHasher.BOOL, CustomModelData::flags)
            .optionalList("strings", MinecraftHasher.STRING, CustomModelData::strings)
            .optionalList("colors", MinecraftHasher.INT, CustomModelData::colors));

        registerMap(DataComponentTypes.TOOLTIP_DISPLAY, builder -> builder
            .optional("hide_tooltip", MinecraftHasher.BOOL, TooltipDisplay::hideTooltip, false)
            .optionalList("hidden_components", RegistryHasher.DATA_COMPONENT_TYPE, TooltipDisplay::hiddenComponents));

        registerInt(DataComponentTypes.REPAIR_COST);

        register(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, MinecraftHasher.BOOL);
        //register(DataComponentTypes.INTANGIBLE_PROJECTILE); // TODO MCPL is wrong

        registerMap(DataComponentTypes.FOOD, builder -> builder
            .accept("nutrition", MinecraftHasher.INT, FoodProperties::getNutrition)
            .accept("saturation", MinecraftHasher.FLOAT, FoodProperties::getSaturationModifier)
            .optional("can_always_eat", MinecraftHasher.BOOL, FoodProperties::isCanAlwaysEat, false));
        registerMap(DataComponentTypes.CONSUMABLE, builder -> builder
            .optional("consume_seconds", MinecraftHasher.FLOAT, Consumable::consumeSeconds, 1.6F)
            .optional("animation", MinecraftHasher.ITEM_USE_ANIMATION, Consumable::animation, Consumable.ItemUseAnimation.EAT)
            .optional("sound", RegistryHasher.SOUND_EVENT, Consumable::sound, BuiltinSound.ENTITY_GENERIC_EAT)
            .optional("has_consume_particles", MinecraftHasher.BOOL, Consumable::hasConsumeParticles, true)); // TODO consume effect needs identifier in MCPL

        register(DataComponentTypes.USE_REMAINDER, RegistryHasher.ITEM_STACK);

        registerMap(DataComponentTypes.USE_COOLDOWN, builder -> builder
            .accept("seconds", MinecraftHasher.FLOAT, UseCooldown::seconds)
            .optionalNullable("cooldown_group", MinecraftHasher.KEY, UseCooldown::cooldownGroup));
        registerMap(DataComponentTypes.DAMAGE_RESISTANT, builder -> builder
            .accept("types", MinecraftHasher.TAG, Function.identity()));
        registerMap(DataComponentTypes.TOOL, builder -> builder
            .acceptList("rules", RegistryHasher.TOOL_RULE, ToolData::getRules)
            .optional("default_mining_speed", MinecraftHasher.FLOAT, ToolData::getDefaultMiningSpeed, 1.0F)
            .optional("damage_per_block", MinecraftHasher.INT, ToolData::getDamagePerBlock, 1)
            .optional("can_destroy_blocks_in_creative", MinecraftHasher.BOOL, ToolData::isCanDestroyBlocksInCreative, true));
        registerMap(DataComponentTypes.WEAPON, builder -> builder
            .optional("item_damage_per_attack", MinecraftHasher.INT, Weapon::itemDamagePerAttack, 1)
            .optional("disable_blocking_for_seconds", MinecraftHasher.FLOAT, Weapon::disableBlockingForSeconds, 0.0F));
        registerMap(DataComponentTypes.ENCHANTABLE, builder -> builder
            .accept("value", MinecraftHasher.INT, Function.identity()));
        registerMap(DataComponentTypes.EQUIPPABLE, builder -> builder
            .accept("slot", MinecraftHasher.EQUIPMENT_SLOT, Equippable::slot)
            .optional("equip_sound", RegistryHasher.SOUND_EVENT, Equippable::equipSound, BuiltinSound.ITEM_ARMOR_EQUIP_GENERIC)
            .optionalNullable("asset_id", MinecraftHasher.KEY, Equippable::model)
            .optionalNullable("camera_overlay", MinecraftHasher.KEY, Equippable::cameraOverlay)
            .optionalNullable("allowed_entities", RegistryHasher.ENTITY_TYPE.holderSet(), Equippable::allowedEntities)
            .optional("dispensable", MinecraftHasher.BOOL, Equippable::dispensable, true)
            .optional("swappable", MinecraftHasher.BOOL, Equippable::dispensable, true)
            .optional("damage_on_hurt", MinecraftHasher.BOOL, Equippable::damageOnHurt, true)
            .optional("equip_on_interact", MinecraftHasher.BOOL, Equippable::equipOnInteract, false));
        registerMap(DataComponentTypes.REPAIRABLE, builder -> builder
            .accept("items", RegistryHasher.ITEM.holderSet(), Function.identity()));

        registerUnit(DataComponentTypes.GLIDER);
        register(DataComponentTypes.TOOLTIP_STYLE, MinecraftHasher.KEY);

        registerMap(DataComponentTypes.DEATH_PROTECTION, builder -> builder); // TODO consume effect needs identifier in MCPL
        registerMap(DataComponentTypes.BLOCKS_ATTACKS, builder -> builder); // TODO needs damage types, add a way to cache identifiers without reading objects in registrycache
        register(DataComponentTypes.STORED_ENCHANTMENTS, MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).convert(ItemEnchantments::getEnchantments)); // TODO duplicate code?

        registerInt(DataComponentTypes.DYED_COLOR);
        registerInt(DataComponentTypes.MAP_COLOR);
        registerInt(DataComponentTypes.MAP_ID);
        register(DataComponentTypes.MAP_DECORATIONS, MinecraftHasher.MNBT);

        // TODO charged projectiles also need the recursion™
        // TODO same for bundle contents

        registerMap(DataComponentTypes.POTION_CONTENTS, builder -> builder
            .optional("potion", RegistryHasher.POTION, PotionContents::getPotionId, -1)
            .optional("custom_color", MinecraftHasher.INT, PotionContents::getCustomColor, -1)
            .optionalList("custom_effects", RegistryHasher.MOB_EFFECT_INSTANCE, PotionContents::getCustomEffects)
            .optionalNullable("custom_name", MinecraftHasher.STRING, PotionContents::getCustomName));

        register(DataComponentTypes.POTION_DURATION_SCALE, MinecraftHasher.FLOAT);
    }

    private static void registerUnit(DataComponentType<Unit> component) {
        register(component, MinecraftHasher.UNIT);
    }

    private static void registerInt(IntComponentType component) {
        register(component, MinecraftHasher.INT);
    }

    private static <T> void registerMap(DataComponentType<T> component, UnaryOperator<MapHasher<T>> builder) {
        register(component, MinecraftHasher.mapBuilder(builder));
    }

    private static <T> void register(DataComponentType<T> component, MinecraftHasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> MinecraftHasher<T> hasherOrEmpty(DataComponentType<T> component) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            return MinecraftHasher.UNIT.convert(value -> Unit.INSTANCE);
        }
        return hasher;
    }

    public static <T> HashCode hash(DataComponentType<T> component, T value) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!"); // TODO we might not have hashers for every component, in which case, fix this
        }
        return hasher.hash(value, new MinecraftHashEncoder());
    }
}
