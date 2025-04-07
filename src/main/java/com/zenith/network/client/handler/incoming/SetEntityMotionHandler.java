package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.MODULE;

public class SetEntityMotionHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityMotionPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetEntityMotionPacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer() && packet.getEntityId() == CACHE.getPlayerCache().getEntityId()) {
            MODULE.get(PlayerSimulation.class).handleSetMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        }
        return true;
    }
}
