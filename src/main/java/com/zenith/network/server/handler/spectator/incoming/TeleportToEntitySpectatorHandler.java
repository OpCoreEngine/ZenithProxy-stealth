package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.api.network.PacketHandler;
import com.zenith.api.network.server.ServerSession;
import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorSync;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;

import static com.zenith.Globals.CACHE;

public class TeleportToEntitySpectatorHandler implements PacketHandler<ServerboundTeleportToEntityPacket, ServerSession> {
    @Override
    public ServerboundTeleportToEntityPacket apply(final ServerboundTeleportToEntityPacket packet, final ServerSession session) {
        final Entity targetEntity = CACHE.getEntityCache().get(packet.getTarget());
        if (targetEntity != null) {
            if (session.hasCameraTarget()) {
                session.setCameraTarget(null);
                session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
            }
            SpectatorSync.syncSpectatorPositionToEntity(session, targetEntity);
        }
        return null;
    }
}
