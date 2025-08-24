package com.zenith.feature.autosolver;

import com.zenith.module.impl.AutoSolver;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundOpenSignEditorPacket;

import static com.zenith.Globals.MODULE;

public class AutoSolverSignEditorHandler implements ClientEventLoopPacketHandler<ClientboundOpenSignEditorPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundOpenSignEditorPacket packet, final ClientSession session) {
        AutoSolver autoSolver = MODULE.get(AutoSolver.class);
        if (autoSolver != null) {
            autoSolver.handleOpenSignEditor(packet);
        }
        return true;
    }
}