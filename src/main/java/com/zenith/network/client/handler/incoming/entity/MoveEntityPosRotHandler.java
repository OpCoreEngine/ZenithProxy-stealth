package com.zenith.network.client.handler.incoming.entity;

import com.zenith.api.network.ClientEventLoopPacketHandler;
import com.zenith.api.network.client.ClientSession;
import com.zenith.cache.data.entity.Entity;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class MoveEntityPosRotHandler implements ClientEventLoopPacketHandler<ClientboundMoveEntityPosRotPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundMoveEntityPosRotPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity == null) return false;
        entity.setYaw(packet.getYaw())
            .setPitch(packet.getPitch())
            .setX(entity.getX() + packet.getMoveX())
            .setY(entity.getY() + packet.getMoveY())
            .setZ(entity.getZ() + packet.getMoveZ());
        return true;
    }
}
