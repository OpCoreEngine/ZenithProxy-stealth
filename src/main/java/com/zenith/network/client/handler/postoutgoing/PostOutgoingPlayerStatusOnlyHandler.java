package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;

public class PostOutgoingPlayerStatusOnlyHandler implements ClientEventLoopPacketHandler<ServerboundMovePlayerStatusOnlyPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundMovePlayerStatusOnlyPacket packet, final ClientSession session) {
        // todo: cache onground
        return true;
    }
}
