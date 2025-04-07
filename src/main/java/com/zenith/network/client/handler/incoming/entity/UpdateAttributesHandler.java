package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static java.util.Objects.isNull;

public class UpdateAttributesHandler implements ClientEventLoopPacketHandler<ClientboundUpdateAttributesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundUpdateAttributesPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (isNull(entity)) return false;
        entity.updateAttributes(packet.getAttributes());
        return true;
    }
}
