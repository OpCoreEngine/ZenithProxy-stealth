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

import com.zenith.mc.Registry;
import com.zenith.mc.RegistryData;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.enchantment.EnchantmentRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.potion.PotionRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.Arrays;

public interface RegistryHasher extends MinecraftHasher<Integer> {

    RegistryHasher BLOCK = registry(BlockRegistry.REGISTRY);

    RegistryHasher ITEM = registry(ItemRegistry.REGISTRY);

    RegistryHasher ENTITY_TYPE = enumIdRegistry(EntityType.values());

    RegistryHasher ENCHANTMENT = registry(EnchantmentRegistry.REGISTRY);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.convert(DataComponentType::getKey);

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT = (component, encoder) -> {
        MinecraftHasher hasher = ComponentHashers.hasherOrEmpty(component.getType());
        return hasher.hash(component.getValue(), encoder);
    };

    MinecraftHasher<DataComponents> DATA_COMPONENTS = MinecraftHasher.map(RegistryHasher.DATA_COMPONENT_TYPE, DATA_COMPONENT).convert(DataComponents::getDataComponents); // TODO component removals (needs unit value and ! component prefix)

    MinecraftHasher<ItemStack> ITEM_STACK = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", ITEM, ItemStack::getId)
        .accept("count", INT, ItemStack::getAmount)
        .optionalNullable("components", DATA_COMPONENTS, ItemStack::getDataComponents));

    MinecraftHasher<Effect> EFFECT = enumRegistry();

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .optional("amplifier", BYTE, instance -> (byte) instance.getDetails().getAmplifier(), (byte) 0)
        .optional("duration", INT, instance -> instance.getDetails().getDuration(), 0)
        .optional("ambient", BOOL, instance -> instance.getDetails().isAmbient(), false)
        .optional("show_particles", BOOL, instance -> instance.getDetails().isShowParticles(), true)
        .accept("show_icon", BOOL, instance -> instance.getDetails().isShowIcon())); // TODO check this, also hidden effect but is recursive

    RegistryHasher POTION = registry(PotionRegistry.REGISTRY);

    MinecraftHasher<BuiltinSound> BUILTIN_SOUND = KEY.convert(BuiltinSound::getName);

    MinecraftHasher<CustomSound> CUSTOM_SOUND = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_id", KEY, CustomSound::getName)
        .optional("range", FLOAT, CustomSound::getRange, 16.0F));

    MinecraftHasher<Sound> SOUND_EVENT = (sound, encoder) -> {
        if (sound instanceof BuiltinSound builtin) {
            return BUILTIN_SOUND.hash(builtin, encoder);
        }
        return CUSTOM_SOUND.hash((CustomSound) sound, encoder);
    };

    MinecraftHasher<ToolData.Rule> TOOL_RULE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    static RegistryHasher registry(Registry<? extends RegistryData> registry) {
        MinecraftHasher<Integer> hasher = KEY.convert(i -> registry.get(i).name());
        return hasher::hash;
    }

    static <T extends Enum<T>> MinecraftHasher<T> enumRegistry() {
        return KEY.convert(t -> t.name().toLowerCase());
    }

    static <T extends Enum<T>> RegistryHasher enumIdRegistry(T[] values) {
        MinecraftHasher<Integer> hasher = KEY.convert(i -> values[i].name().toLowerCase());
        return hasher::hash;
    }

    default MinecraftHasher<HolderSet> holderSet() {
        return (holder, encoder) -> {
            if (holder.getLocation() != null) {
                return TAG.hash(holder.getLocation(), encoder);
            } else if (holder.getHolders() != null) {
                if (holder.getHolders().length == 1) {
                    return hash(holder.getHolders()[0], encoder);
                }
                return list().hash(Arrays.stream(holder.getHolders()).boxed().toList(), encoder);
            }
            throw new IllegalStateException("HolderSet must have either tag location or holders");
        };
    }
}
