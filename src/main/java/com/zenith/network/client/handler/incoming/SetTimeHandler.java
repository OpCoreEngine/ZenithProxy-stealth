package com.zenith.network.client.handler.incoming;

import com.zenith.event.module.DayTimeChangedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.TPS;
import static com.zenith.Globals.EVENT_BUS;

public class SetTimeHandler implements ClientEventLoopPacketHandler<ClientboundSetTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundSetTimePacket packet, ClientSession session) {
        CACHE.getChunkCache().updateWorldTime(packet);
        TPS.handleTimeUpdate();
        EVENT_BUS.post(new DayTimeChangedEvent());
        return true;
    }
}
