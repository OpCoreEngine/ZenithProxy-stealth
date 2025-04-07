package com.zenith.feature.spectator;

import com.zenith.Proxy;
import com.zenith.api.network.server.ServerSession;
import com.zenith.cache.CachedData;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.feature.spectator.entity.mob.SpectatorEntityEnderDragon;
import com.zenith.feature.spectator.entity.mob.SpectatorEntityPlayerHead;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.zenith.Globals.CACHE;
import static java.util.Arrays.asList;
import static org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode.SPECTATOR;

public final class SpectatorSync {

    public static void syncPlayerEquipmentWithSpectatorsFromCache() {
        sendSpectatorsEquipment();
    }

    public static void syncPlayerPositionWithSpectators() {
        sendSpectatorPackets(SpectatorPacketProvider::playerPosition);
    }

    public static void syncSpectatorPositionToEntity(final ServerSession spectConnection, Entity target) {
        spectConnection.getSpectatorPlayerCache()
            .setX(target.getX())
            .setY(target.getY() + 1) // spawn above entity
            .setZ(target.getZ())
            .setYaw(target.getYaw())
            .setPitch(target.getPitch());
        spectConnection.setAllowSpectatorServerPlayerPosRotate(true);
        spectConnection.send(new ClientboundPlayerPositionPacket(
                spectConnection.getSpectatorPlayerCache().getX(),
                spectConnection.getSpectatorPlayerCache().getY(),
                spectConnection.getSpectatorPlayerCache().getZ(),
                spectConnection.getSpectatorPlayerCache().getYaw(),
                spectConnection.getSpectatorPlayerCache().getPitch(),
                spectConnection.getSpawnTeleportId()
        ));
        spectConnection.setAllowSpectatorServerPlayerPosRotate(false);
        updateSpectatorPosition(spectConnection);
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (!connection.equals(spectConnection) || spectConnection.isShowSelfEntity()) {
                connection.sendAsync(spectConnection.getEntitySpawnPacket());
                connection.sendAsync(spectConnection.getEntityMetadataPacket());
            }
        }
    }

    private static void syncSpectatorPositionToProxiedPlayer(final ServerSession spectConnection) {
        spectConnection.getEventLoop().execute(() -> syncSpectatorPositionToEntity(spectConnection, CACHE.getPlayerCache().getThePlayer()));
    }

    private static void sendSpectatorsEquipment() {
        sendSpectatorPackets(SpectatorPacketProvider::playerEquipment);
    }

    private static void spawnSpectatorForOtherSessions(ServerSession spectatorSession, ServerSession connection) {
        if (!connection.equals(Proxy.getInstance().getCurrentPlayer().get())) {
            spectatorSession.sendAsync(connection.getEntitySpawnPacket());
            spectatorSession.sendAsync(connection.getEntityMetadataPacket());
        }
        connection.sendAsync(spectatorSession.getEntitySpawnPacket());
        connection.sendAsync(spectatorSession.getEntityMetadataPacket());
    }

    public static EntityPlayer getSpectatorPlayerEntity(final ServerSession session) {
        EntityPlayer spectatorEntityPlayer = new EntityPlayer();
        spectatorEntityPlayer.setUuid(session.getSpectatorFakeProfileCache().getProfile().getId());
        spectatorEntityPlayer.setSelfPlayer(true);
        spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
        spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY() + 1); // spawn above player
        spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
        spectatorEntityPlayer.setEntityId(session.getSpectatorSelfEntityId());
        spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
        spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
        spectatorEntityPlayer.getMetadata().put(9, new FloatEntityMetadata(9, MetadataTypes.FLOAT, 20.0f)); // health
        spectatorEntityPlayer.getMetadata().put(17, new ByteEntityMetadata(17, MetadataTypes.BYTE, (byte) 255)); // visible skin parts
        return spectatorEntityPlayer;
    }

    public static void initSpectator(ServerSession session, Supplier<Collection<CachedData>> cacheSupplier) {
        // update spectator player cache
        EntityPlayer spectatorEntityPlayer = getSpectatorPlayerEntity(session);
        session.getSpectatorPlayerCache()
            .setThePlayer(spectatorEntityPlayer)
            .setGameMode(SPECTATOR)
            .setEnableRespawnScreen(CACHE.getPlayerCache().isEnableRespawnScreen())
            .setLastDeathPos(CACHE.getPlayerCache().getLastDeathPos())
            .setPortalCooldown(CACHE.getPlayerCache().getPortalCooldown())
            .setHardcore(CACHE.getPlayerCache().isHardcore())
            .setReducedDebugInfo(CACHE.getPlayerCache().isReducedDebugInfo())
            .setHeldItemSlot(CACHE.getPlayerCache().getHeldItemSlot())
            .setDifficultyLocked(CACHE.getPlayerCache().isDifficultyLocked())
            .setInvincible(true)
            .setCanFly(true)
            .setCreative(false)
            .setFlying(true)
            .setFlySpeed(0.05f)
            .setWalkSpeed(0.1f)
            .setOpLevel(CACHE.getPlayerCache().getOpLevel())
            .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.setAllowSpectatorServerPlayerPosRotate(true);
        DataCache.sendCacheData(cacheSupplier.get(), session);
        session.setAllowSpectatorServerPlayerPosRotate(false);
        session.sendAsync(session.getEntitySpawnPacket());
        session.sendAsync(session.getSelfEntityMetadataPacket());
        SpectatorPacketProvider.playerSpawn().forEach(session::sendAsync);
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (!connection.equals(session)) {
                spawnSpectatorForOtherSessions(session, connection);
                connection.syncTeamMembers();
            }
        }
        session.sendAsync(new ClientboundSetEntityDataPacket(session.getSpectatorSelfEntityId(), new ArrayList<>(spectatorEntityPlayer.getMetadata().values())));
        syncPlayerEquipmentWithSpectatorsFromCache();
    }

    public static void checkSpectatorPositionOutOfRender(final ServerSession spectConnection) {
        if (spectConnection.hasCameraTarget()) return;
        final int spectX = (int) spectConnection.getSpectatorPlayerCache().getX() >> 4;
        final int spectZ = (int) spectConnection.getSpectatorPlayerCache().getZ() >> 4;
        final int playerX = (int) CACHE.getPlayerCache().getX() >> 4;
        final int playerZ = (int) CACHE.getPlayerCache().getZ() >> 4;
        if (Math.abs(spectX - playerX) > (CACHE.getChunkCache().getServerViewDistance() / 2 + 1) || Math.abs(spectZ - playerZ) > (CACHE.getChunkCache().getServerViewDistance() / 2 + 1)) {
            syncSpectatorPositionToProxiedPlayer(spectConnection);
        }
    }

    public static void updateSpectatorPosition(final ServerSession selfSession) {
        if (selfSession.hasCameraTarget()) {
            return;
        }
        double playerEyeHeight = 1.6;
        double specEntityEyeHeight = selfSession.getSpectatorEntity().getEyeHeight();
        double specEntityTotalWidth = selfSession.getSpectatorEntity().getWidth();
        // clamping avoids moving the spectator entity directly in view of the player in 1st person at steep pitches
        float clampedPitch = MathHelper.clamp(selfSession.getSpectatorPlayerCache().getPitch(), -30, 30);

        double distance = (specEntityTotalWidth * -0.5) - 0.5;
        double yawRadians = Math.toRadians(selfSession.getSpectatorPlayerCache().getYaw());
        double pitchRadians = Math.toRadians(clampedPitch);

        double xOffset = -Math.sin(yawRadians) * Math.cos(pitchRadians) * distance;
        double yOffset = -Math.sin(pitchRadians) * distance;
        double zOffset = Math.cos(yawRadians) * Math.cos(pitchRadians) * distance;

        double newX = selfSession.getSpectatorPlayerCache().getX() + xOffset;
        double newY = selfSession.getSpectatorPlayerCache().getY() + playerEyeHeight - specEntityEyeHeight + yOffset;
        double newZ = selfSession.getSpectatorPlayerCache().getZ() + zOffset;
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.sendAsync(new ClientboundTeleportEntityPacket(
                selfSession.getSpectatorEntityId(),
                newX,
                newY,
                newZ,
                getDisplayYaw(selfSession),
                getDisplayPitch(selfSession),
                false
            ));
            connection.sendAsync(new ClientboundRotateHeadPacket(
                selfSession.getSpectatorEntityId(),
                getDisplayYaw(selfSession)
            ));
        }
    }

    public static void sendPlayerSneakStatus() {
        sendSpectatorPackets(SpectatorPacketProvider::playerSneak);
    }

    public static void sendSwing() {
        sendSpectatorPackets(SpectatorPacketProvider::playerSwing);
    }

    public static void sendRespawn() {
        var spectatorConnections = Proxy.getInstance().getSpectatorConnections();
        if (!spectatorConnections.isEmpty()) {
            final List<CachedData> cachedData = asList(CACHE.getChunkCache(), CACHE.getEntityCache(), CACHE.getMapDataCache());
            for (int i = 0; i < spectatorConnections.size(); i++) {
                var connection = spectatorConnections.get(i);
                final List<CachedData> data = new ArrayList<>(4);
                data.addAll(cachedData);
                data.add(connection.getSpectatorPlayerCache());
                SpectatorSync.initSpectator(connection, () -> data);
                // reset playercam back to previous state if any
                if (connection.hasCameraTarget()) {
                    if (connection.getCameraTarget() == CACHE.getPlayerCache().getThePlayer()) {
                        connection.sendAsync(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
                        var connections = Proxy.getInstance().getActiveConnections().getArray();
                        for (int j = 0; j < connections.length; j++) {
                            var session = connections[j];
                            session.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{connection.getSpectatorEntityId()}));
                        }
                    } else { // don't try to go back to playercam if its to a different entity
                        connection.setCameraTarget(null);
                    }
                }
            }
        }
    }

    public static void checkSpectatorPositionOutOfRender(final int chunkX, final int chunkZ) {
        var spectatorConnections = Proxy.getInstance().getSpectatorConnections();
        if (!spectatorConnections.isEmpty()) {
            for (int i = 0; i < spectatorConnections.size(); i++) {
                var connection = spectatorConnections.get(i);
                if (connection.hasCameraTarget()) continue;
                final int spectX = (int) connection.getSpectatorPlayerCache().getX() >> 4;
                final int spectZ = (int) connection.getSpectatorPlayerCache().getZ() >> 4;
                if ((spectX == chunkX || spectX + 1 == chunkX || spectX - 1 == chunkX)
                    && (spectZ == chunkZ || spectZ + 1 == chunkZ || spectZ - 1 == chunkZ)) {
                    SpectatorSync.syncSpectatorPositionToProxiedPlayer(connection);
                }
            }
        }
    }

    public static float getDisplayYaw(final ServerSession serverConnection) {
        // idk why but dragon is displayed 180 degrees off from what you'd expect
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityEnderDragon) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityPlayerHead) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else {
            return serverConnection.getSpectatorPlayerCache().getYaw();
        }
    }

    public static float getDisplayPitch(final ServerSession serverConnection) {
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityPlayerHead) {
            return -serverConnection.getSpectatorPlayerCache().getPitch();
        }
        return serverConnection.getSpectatorPlayerCache().getPitch();
    }

    private static void sendSpectatorPackets(final Supplier<List<Packet>> packetProvider) {
        var spectatorConnections = Proxy.getInstance().getSpectatorConnections();
        if (!spectatorConnections.isEmpty()) {
            var packets = packetProvider.get();
            for (int i = 0; i < spectatorConnections.size(); i++) {
                var connection = spectatorConnections.get(i);
                packets.forEach(connection::sendAsync);
            }
        }
    }
}
