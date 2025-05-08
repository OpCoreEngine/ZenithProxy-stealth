package com.zenith.network.client.handler.incoming.level;

import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.EVENT_BUS;

public class GameEventHandler implements ClientEventLoopPacketHandler<ClientboundGameEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundGameEventPacket packet, @NonNull ClientSession session) {
        switch (packet.getNotification()) {
            case CHANGE_GAME_MODE -> CACHE.getPlayerCache().setGameMode((GameMode) packet.getValue());
            case START_RAINING -> {
                CACHE.getChunkCache().setRaining(true);
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case STOP_RAINING -> {
                CACHE.getChunkCache().setRaining(false);
                CACHE.getChunkCache().setThunderStrength(0.0f);
                CACHE.getChunkCache().setRainStrength(0.0f);
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case RAIN_LEVEL_CHANGE -> {
                CACHE.getChunkCache().setRainStrength(((RainStrengthValue) packet.getValue()).getStrength());
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case THUNDER_LEVEL_CHANGE -> {
                CACHE.getChunkCache().setThunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case IMMEDIATE_RESPAWN -> CACHE.getPlayerCache()
                .setEnableRespawnScreen(packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN);
        }
        return true;
    }
}
