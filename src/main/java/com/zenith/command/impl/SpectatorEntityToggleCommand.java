package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;

import static com.zenith.util.ComponentSerializer.minimessage;

public class SpectatorEntityToggleCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("entityToggle")
            .category(CommandCategory.MANAGE)
            .description("Toggles the visibility of spectator entities. Only usable by spectators.")
            .aliases("etoggle")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("entityToggle").requires(c -> Command.validateCommandSource(c, CommandSources.SPECTATOR)).executes(c -> {
            var session = c.getSource().getInGamePlayerInfo().session();
            session.setShowSelfEntity(!session.isShowSelfEntity());
            if (session.isShowSelfEntity()) {
                session.sendAsync(session.getEntitySpawnPacket());
                session.sendAsync(session.getEntityMetadataPacket());
            } else {
                session.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
            }
            session.sendAsyncMessage(minimessage("<blue>Show self entity toggled " + (session.isShowSelfEntity() ? "on!" : "off!")));
            c.getSource().setNoOutput(true);
        });
    }
}
