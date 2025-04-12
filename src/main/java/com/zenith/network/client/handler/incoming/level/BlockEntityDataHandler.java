package com.zenith.network.client.handler.incoming.level;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class BlockEntityDataHandler implements ClientEventLoopPacketHandler<ClientboundBlockEntityDataPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundBlockEntityDataPacket packet, @NonNull ClientSession session) {
        if (!CACHE.getChunkCache().updateBlockEntity(packet)) {
            CLIENT_LOG.warn("Received ServerUpdateTileEntityPacket for chunk column that does not exist: [{}, {}, {}], data: {}", packet.getX(), packet.getY(), packet.getZ(), packet.getNbt());
            return false;
        }
        return true;
    }
}
