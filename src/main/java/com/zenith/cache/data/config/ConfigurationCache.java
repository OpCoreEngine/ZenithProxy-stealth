package com.zenith.cache.data.config;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Configuration phase registry and features cache
 */
@Data
public class ConfigurationCache implements CachedData {
    private Map<String, List<RegistryEntry>> registryEntries = new ConcurrentHashMap<>();
    protected String[] enabledFeatures = new String[]{"minecraft:vanilla"};
    private Map<UUID, ResourcePack> resourcePacks = new ConcurrentHashMap<>();
    private Map<String, Map<String, int[]>> tags = new ConcurrentHashMap<>();

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        registryEntries.forEach((registry, entries) -> consumer.accept(new ClientboundRegistryDataPacket(registry, entries)));
        consumer.accept(new ClientboundUpdateEnabledFeaturesPacket(this.enabledFeatures));
        // todo: we need to make the zenith mc server wait until the player has sent a response to the resource pack prompt(s)
//        resourcePacks.forEach((uuid, resourcePack) -> consumer.accept(new ClientboundResourcePackPushPacket(
//            resourcePack.id(),
//            resourcePack.url(),
//            resourcePack.hash(),
//            resourcePack.required(),
//            resourcePack.prompt()
//        )));
        consumer.accept(new ClientboundUpdateTagsPacket(this.tags));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL) {
            this.registryEntries.clear();
            this.enabledFeatures = new String[]{"minecraft:vanilla"};
            this.resourcePacks = new ConcurrentHashMap<>();
            this.tags = new ConcurrentHashMap<>();
        }
    }
}
