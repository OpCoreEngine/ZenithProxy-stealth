package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.module.impl.GlowingEffect;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.MODULE;

public class SetEntityDataHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEntityDataPacket packet, @NonNull ClientSession session) {
        // Apply glowing effect modification before processing
        MODULE.get(GlowingEffect.class).interceptEntityDataPacket(packet);
        
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            for (int i = 0; i < packet.getMetadata().size(); i++) {
                var metadata = packet.getMetadata().get(i);
                entity.getMetadata().put(metadata.getId(), metadata);
            }
        }
        return true;
    }
}
