package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundAwardStatsPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class AwardStatsHandler implements ClientEventLoopPacketHandler<ClientboundAwardStatsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundAwardStatsPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache().getStatistics().putAll(packet.getStatistics());
        return true;
    }
}
