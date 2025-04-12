package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import com.zenith.util.Config;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundPongResponsePacket;

import static com.zenith.Globals.CONFIG;

public class PongResponseHandler implements PacketHandler<ClientboundPongResponsePacket, ClientSession> {
    public static final PongResponseHandler INSTANCE = new PongResponseHandler();
    @Override
    public ClientboundPongResponsePacket apply(final ClientboundPongResponsePacket packet, final ClientSession session) {
        if (CONFIG.client.ping.mode == Config.Client.Ping.Mode.PACKET && session.getLastPingId() == packet.getPingTime()) {
            // this is from our own ping task
            session.setPing(System.currentTimeMillis() - session.getLastPingSentTime());
            return null;
        }
        return packet;
    }
}
