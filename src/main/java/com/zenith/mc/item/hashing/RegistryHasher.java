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
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.Arrays;
import java.util.Map;

import static com.zenith.Shared.CLIENT_LOG;

public interface RegistryHasher extends MinecraftHasher<Integer> {

    RegistryHasher BLOCK = registry(BlockRegistry.REGISTRY);

    RegistryHasher ITEM = registry(ItemRegistry.REGISTRY);

    RegistryHasher ENTITY_TYPE = enumIdRegistry(EntityType.values());

    RegistryHasher ENCHANTMENT = registry(EnchantmentRegistry.REGISTRY);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.convert(DataComponentType::getKey);

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT = (component, encoder) -> {
        MinecraftHasher hasher = DataComponentHashers.hasherOrEmpty(component.getType());
        return hasher.hash(component.getValue(), encoder);
    };

    MinecraftHasher<DataComponents> DATA_COMPONENTS = MinecraftHasher.map(RegistryHasher.DATA_COMPONENT_TYPE, DATA_COMPONENT).convert(DataComponents::getDataComponents); // TODO component removals (needs unit value and ! component prefix)

    MinecraftHasher<ItemStack> ITEM_STACK = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", ITEM, ItemStack::getId)
        .accept("count", INT, ItemStack::getAmount)
        .optionalNullable("components", DATA_COMPONENTS, ItemStack::getDataComponents));

    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher EFFECT_ID = enumIdRegistry(Effect.values());

    MinecraftHasher<SuspiciousStewEffect> SUSPICIOUS_STEW_EFFECT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", EFFECT_ID, SuspiciousStewEffect::getMobEffectId)
        .optional("duration", INT, SuspiciousStewEffect::getDuration, 160));


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

    MinecraftHasher<InstrumentComponent.Instrument> DIRECT_INSTRUMENT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, InstrumentComponent.Instrument::soundEvent)
        .accept("use_duration", FLOAT, InstrumentComponent.Instrument::useDuration)
        .accept("range", FLOAT, InstrumentComponent.Instrument::range)
        .accept("description", ComponentHasher.COMPONENT, InstrumentComponent.Instrument::description));

    MinecraftHasher<Holder<InstrumentComponent.Instrument>> INSTRUMENT = holderIdOnly(DIRECT_INSTRUMENT);

    MinecraftHasher<InstrumentComponent> INSTRUMENT_COMPONENT = MinecraftHasher.either(INSTRUMENT, InstrumentComponent::instrumentHolder, KEY, InstrumentComponent::instrumentLocation);


    MinecraftHasher<ToolData.Rule> TOOL_RULE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    MinecraftHasher<Map<String, String>> TRIM_MATERIAL_ASSET_OVERRIDES = MinecraftHasher.map(KEY, STRING);

    MinecraftHasher<ArmorTrim.TrimMaterial> DIRECT_TRIM_MATERIAL = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_name", MinecraftHasher.STRING, ArmorTrim.TrimMaterial::assetBase)
        .optional("override_armor_assets", TRIM_MATERIAL_ASSET_OVERRIDES, ArmorTrim.TrimMaterial::assetOverrides, Map.of())
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimMaterial::description));

    MinecraftHasher<Holder<ArmorTrim.TrimMaterial>> TRIM_MATERIAL = holderIdOnly(DIRECT_TRIM_MATERIAL);

    MinecraftHasher<ProvidesTrimMaterial> PROVIDES_TRIM_MATERIAL = MinecraftHasher.either(TRIM_MATERIAL, ProvidesTrimMaterial::materialHolder, KEY, ProvidesTrimMaterial::materialLocation);

    MinecraftHasher<ArmorTrim.TrimPattern> DIRECT_TRIM_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, ArmorTrim.TrimPattern::assetId)
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimPattern::description)
        .accept("decal", BOOL, ArmorTrim.TrimPattern::decal));

    MinecraftHasher<Holder<ArmorTrim.TrimPattern>> TRIM_PATTERN = holderIdOnly(DIRECT_TRIM_PATTERN);

    MinecraftHasher<ArmorTrim> ARMOR_TRIM = MinecraftHasher.mapBuilder(builder -> builder
        .accept("material", TRIM_MATERIAL, ArmorTrim::material)
        .accept("pattern", TRIM_PATTERN, ArmorTrim::pattern));

    MinecraftHasher<JukeboxPlayable.JukeboxSong> DIRECT_JUKEBOX_SONG = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, JukeboxPlayable.JukeboxSong::soundEvent)
        .accept("description", ComponentHasher.COMPONENT, JukeboxPlayable.JukeboxSong::description)
        .accept("length_in_seconds", FLOAT, JukeboxPlayable.JukeboxSong::lengthInSeconds)
        .accept("comparator_output", INT, JukeboxPlayable.JukeboxSong::comparatorOutput));

    MinecraftHasher<Holder<JukeboxPlayable.JukeboxSong>> JUKEBOX_SONG = holderIdOnly(DIRECT_JUKEBOX_SONG);

    MinecraftHasher<JukeboxPlayable> JUKEBOX_PLAYABLE = MinecraftHasher.either(JUKEBOX_SONG, JukeboxPlayable::songHolder, KEY, JukeboxPlayable::songLocation);

    static RegistryHasher registry(Registry<? extends RegistryData> registry) {
        MinecraftHasher<Integer> hasher = KEY.convert(i -> registry.get(i).name());
        return hasher::hash;
    }

    // We don't need the registry generic type, and this works easier for goat horn instruments and other registries
    static <T> MinecraftHasher<Holder<T>> holder(Registry<? extends RegistryData> registry, MinecraftHasher<T> direct) {
        RegistryHasher registryHasher = registry(registry);
        return (value, encoder) -> {
            if (value.isId()) {
                return registryHasher.hash(value.id(), encoder);
            }
            return direct.hash(value.custom(), encoder);
        };
    }

    static <T> MinecraftHasher<Holder<T>> holderIdOnly(MinecraftHasher<T> direct) {
        return (value, encoder) -> {
            if (value.isId()) {
                CLIENT_LOG.error("Holder type: {} is id: {} but no registry cached", value.getClass().getSimpleName(), value.id());
                return INT.hash(value.id(), encoder); // this is completely incorrect but idk lol
            }
            return direct.hash(value.custom(), encoder);
        };
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
