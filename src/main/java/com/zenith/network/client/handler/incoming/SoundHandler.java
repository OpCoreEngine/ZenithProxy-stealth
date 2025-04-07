package com.zenith.network.client.handler.incoming;

import com.zenith.event.module.SplashSoundEffectEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;

import static com.zenith.Globals.EVENT_BUS;
import static org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound.ENTITY_FISHING_BOBBER_SPLASH;

public class SoundHandler implements ClientEventLoopPacketHandler<ClientboundSoundPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSoundPacket packet, ClientSession session) {
        if (packet.getSound() == ENTITY_FISHING_BOBBER_SPLASH) EVENT_BUS.postAsync(new SplashSoundEffectEvent(packet));
        return true;
    }
}
