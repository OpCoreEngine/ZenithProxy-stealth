package com.zenith.network.registry;

import com.zenith.util.Config.Debug.PacketLog.PacketLogConfig;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.function.Supplier;

import static com.zenith.Globals.CONFIG;

public class PacketLogPacketHandlerCodec extends PacketHandlerCodec {
    private final Supplier<PacketLogConfig> configSupplier;
    private final Logger logger;

    public PacketLogPacketHandlerCodec(
        final String id,
        final Logger logger,
        Supplier<PacketLogConfig> configSupplier
    ) {
        super(Integer.MAX_VALUE, id, new EnumMap<>(ProtocolState.class), (session) -> CONFIG.debug.packetLog.enabled);
        this.logger = logger;
        this.configSupplier = configSupplier;
    }

    private void log(String message, Object... args) {
        if (CONFIG.debug.packetLog.logLevelDebug)
            logger.debug(message, args);
        else
            logger.info(message, args);
    }

    @Override
    public <P extends Packet, S extends Session> P handleInbound(@NonNull P packet, @NonNull S session) {
        if (configSupplier.get().received && shouldLog(packet))
            log("[{}] [{}] Received: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), configSupplier.get().receivedBody ? packet : packet.getClass().getSimpleName());
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        if (configSupplier.get().preSent && shouldLog(packet))
            log("[{}] [{}] Sending: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), configSupplier.get().preSentBody ? packet : packet.getClass().getSimpleName());
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        if (configSupplier.get().postSent && shouldLog(packet))
            log("[{}] [{}] Sent: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), configSupplier.get().postSentBody ? packet : packet.getClass().getSimpleName());
    }

    private boolean shouldLog(Packet packet) {
        return CONFIG.debug.packetLog.packetFilter.isEmpty()
            || packet.getClass().getSimpleName().toLowerCase().contains(CONFIG.debug.packetLog.packetFilter.toLowerCase());
    }
}
