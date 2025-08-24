package com.zenith.feature.autosolver;

import com.zenith.module.impl.AutoSolver;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

import static com.zenith.Globals.MODULE;

public class AutoSolverOpenScreenHandler implements ClientEventLoopPacketHandler<ClientboundOpenScreenPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundOpenScreenPacket packet, final ClientSession session) {
        AutoSolver autoSolver = MODULE.get(AutoSolver.class);
        if (autoSolver != null) {
            autoSolver.handleOpenScreen(packet);
        }
        return true;
    }
}