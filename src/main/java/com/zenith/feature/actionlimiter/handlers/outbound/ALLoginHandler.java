package com.zenith.feature.actionlimiter.handlers.outbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;

public class ALLoginHandler implements PacketHandler<ClientboundLoginPacket, ServerSession> {
    @Override
    public ClientboundLoginPacket apply(final ClientboundLoginPacket packet, final ServerSession session) {
        if (CONFIG.client.extra.actionLimiter.allowMovement)
            return packet;
        int playerX = (int) CACHE.getPlayerCache().getX();
        int playerY = (int) CACHE.getPlayerCache().getY();
        int playerZ = (int) CACHE.getPlayerCache().getZ();
        if (playerY <= CONFIG.client.extra.actionLimiter.movementMinY) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        if (MathHelper.distance2d(CONFIG.client.extra.actionLimiter.movementHomeX,
                                  CONFIG.client.extra.actionLimiter.movementHomeZ,
                                  playerX,
                                  playerZ) > CONFIG.client.extra.actionLimiter.movementDistance) {
            session.disconnect("ActionLimiter: Movement not allowed");
            return null;
        }
        return packet;
    }
}
