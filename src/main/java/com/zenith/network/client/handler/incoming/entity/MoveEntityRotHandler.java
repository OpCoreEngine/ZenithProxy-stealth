package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class MoveEntityRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityRotPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity == null) return false;
        entity.setYaw(packet.getYaw())
            .setPitch(packet.getPitch());
        return true;
    }
}
