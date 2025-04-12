package com.zenith.network.codec;

import com.zenith.network.client.ClientSession;
import io.netty.channel.EventLoop;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.concurrent.RejectedExecutionException;

import static com.zenith.Globals.CLIENT_LOG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@FunctionalInterface
public interface ClientEventLoopPacketHandler<P extends Packet, S extends ClientSession> extends PacketHandler<P, S> {

    boolean applyAsync(P packet, S session);

    default P apply(P packet, S session) {
        if (packet == null) return null;
        try {
            EventLoop clientEventLoop = session.getClientEventLoop();
            if (clientEventLoop.inEventLoop()) {
                applyWithRetries(packet, session, 0);
            } else {
                clientEventLoop.execute(() -> applyWithRetries(packet, session, 0));
            }
        } catch (final RejectedExecutionException e) {
            // fall through
        }
        return packet;
    }

    private void applyWithRetries(P packet, S session, final int tryCount) {
        try {
            if (!applyAsync(packet, session)) {
                if (tryCount > 1) {
                    CLIENT_LOG.debug("Unable to apply async handler for packet: {}", packet.getClass().getSimpleName());
                    return;
                }
                session.getClientEventLoop().schedule(() -> applyWithRetries(packet, session, tryCount + 1), 250, MILLISECONDS);
            }
        } catch (final RejectedExecutionException e) {
            // fall through
        } catch (final Throwable e) {
            CLIENT_LOG.error("Async handler error", e);
        }
    }
}
