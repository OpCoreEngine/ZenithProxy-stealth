package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;

public class MoveVehicleSpectatorOutgoingHandler implements PacketHandler<ClientboundMoveVehiclePacket, ServerSession> {
    @Override
    public ClientboundMoveVehiclePacket apply(ClientboundMoveVehiclePacket packet, ServerSession session) {
        return null;
    }
}
