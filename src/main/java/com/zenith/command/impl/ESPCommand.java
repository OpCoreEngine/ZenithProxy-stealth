package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.module.impl.ESP;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ESPCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("esp")
            .category(CommandCategory.MODULE)
            .description("Renders the spectral effect around all entities")
            .usageLines("on/off")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("esp")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.extra.esp.enable = getToggle(c, "toggle");
                MODULE.get(ESP.class).syncEnabledFromConfig();
                var player = Proxy.getInstance().getActivePlayer();
                if (player != null) {
                    // resend all entity metadata from cache
                    CACHE.getEntityCache().getEntities().values().forEach(e -> {
                        EntityMetadata<?, ?> toSend;
                        toSend = e.getMetadata().get(0);
                        if (toSend == null)
                            toSend = new ByteEntityMetadata(0, MetadataTypes.BYTE, (byte) 0);
                        player.sendAsync(new ClientboundSetEntityDataPacket(e.getEntityId(), asList(toSend)));
                    });
                }
                c.getSource().getEmbed()
                    .title("ESP " + toggleStrCaps(CONFIG.server.extra.esp.enable))
                    .primaryColor();
                return OK;
            }));
    }
}
