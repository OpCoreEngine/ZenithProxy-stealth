package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.KeepAliveTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.handler.ProxyServerLoginHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EXECUTOR;

public class FinishConfigurationHandler implements PacketHandler<ServerboundFinishConfigurationPacket, ServerSession> {
    @Override
    public ServerboundFinishConfigurationPacket apply(final ServerboundFinishConfigurationPacket packet, final ServerSession session) {
        session.switchInboundState(ProtocolState.GAME);
        if (!session.isConfigured()) {
            ProxyServerLoginHandler.INSTANCE.loggedIn(session);
            if (CONFIG.client.automaticKeepAliveManagement) {
                EXECUTOR.execute(new KeepAliveTask(session));
            }
            return null;
        }
        return packet;
    }
}
