package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundTrackedWaypointPacket;

/**
 * Filters out waypoint packets for spectators to prevent client crashes
 * on versions that don't handle waypoints properly
 */
public class TrackedWaypointSpectatorHandler implements PacketHandler<ClientboundTrackedWaypointPacket, ServerSession> {
    @Override
    public ClientboundTrackedWaypointPacket apply(ClientboundTrackedWaypointPacket packet, ServerSession session) {
        // Don't send waypoint packets to spectators
        // This prevents crashes on some client versions
        return null;
    }
}