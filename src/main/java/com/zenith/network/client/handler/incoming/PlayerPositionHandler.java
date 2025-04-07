package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.feature.player.PlayerSimulation;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.module.impl.AntiAFK;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.*;
import static java.util.Objects.isNull;

public class PlayerPositionHandler implements ClientEventLoopPacketHandler<ClientboundPlayerPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerPositionPacket packet, @NonNull ClientSession session) {
        PlayerCache cache = CACHE.getPlayerCache();
        var teleportQueue = cache.getTeleportQueue();
        cache.getTeleportQueue().enqueue(packet.getTeleportId());
        while (teleportQueue.size() > 100) {
            var id = teleportQueue.dequeueInt();
            CLIENT_LOG.debug("Teleport queue larger than 100, dropping oldest entry. Dropped teleport: {} Last teleport: {}", id, packet.getTeleportId());
        }
        cache
            .setRespawning(false)
            .setX((packet.getRelative().contains(PositionElement.X) ? cache.getX() : 0.0d) + packet.getX())
            .setY((packet.getRelative().contains(PositionElement.Y) ? cache.getY() : 0.0d) + packet.getY())
            .setZ((packet.getRelative().contains(PositionElement.Z) ? cache.getZ() : 0.0d) + packet.getZ())
            .setYaw((packet.getRelative().contains(PositionElement.YAW) ? cache.getYaw() : 0.0f) + packet.getYaw())
            .setPitch((packet.getRelative().contains(PositionElement.PITCH) ? cache.getPitch() : 0.0f) + packet.getPitch());
        ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (isNull(currentPlayer) || !currentPlayer.isLoggedIn()) {
            PlayerSimulation.INSTANCE.handlePlayerPosRotate(packet.getTeleportId());
        } else {
            CLIENT_LOG.debug("Passing teleport {} through to current player", packet.getTeleportId());
        }
        Baritone.INSTANCE.onPlayerPosRotate();
        SpectatorSync.syncPlayerPositionWithSpectators();
        MODULE.get(AntiAFK.class).handlePlayerPosRotate();
        return true;
    }
}
