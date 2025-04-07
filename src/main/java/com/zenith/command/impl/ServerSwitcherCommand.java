package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.api.command.*;
import com.zenith.discord.Embed;
import com.zenith.network.server.ServerSession;
import com.zenith.util.Config.Server.Extra.ServerSwitcher.ServerSwitcherServer;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class ServerSwitcherCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("switch")
            .category(CommandCategory.MODULE)
            .description("""
            Switch the connected player to an alternate MC server.
            
            Can be used to switch between multiple ZenithProxy instances quickly.
            
            Servers being switched to must have transfers enabled and be on an MC version >=1.20.6
            """)
            .usageLines(
                "register <name> <address> <port>",
                "list",
                "<name>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("switch")
            .then(literal("register")
                .then(argument("name", wordWithChars())
                    .then(argument("address", wordWithChars())
                        .then(argument("port", integer(1, 65535)).executes(c -> {
                            var name = getString(c, "name");
                            var address = getString(c, "address");
                            var port = getInteger(c, "port");
                            var newServer = new ServerSwitcherServer(name, address, port);
                            var servers = CONFIG.server.extra.serverSwitcher.servers;
                            servers.removeIf(s -> s.name().equalsIgnoreCase(newServer.name()));
                            servers.add(newServer);
                            c.getSource().getEmbed()
                                .title("Server registered");
                            return OK;
                        })))))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Server List");
                return OK;
            }))
            .then(argument("name", wordWithChars()).requires(context -> validateCommandSource(context, CommandSource.IN_GAME_PLAYER)).executes(c -> {
                var name = getString(c, "name");
                var server = CONFIG.server.extra.serverSwitcher.servers.stream()
                    .filter(s -> s.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

                if (server == null) {
                    c.getSource().getEmbed()
                        .title("Server not found");
                    return OK;
                }
                ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
                if (currentPlayer == null) {
                    c.getSource().getEmbed()
                        .title("No player connected to transfer");
                    return OK;
                }
                if (currentPlayer.getProtocolVersion().olderThan(ProtocolVersion.v1_20_5)) {
                    c.getSource().getEmbed()
                        .title("Unsupported Client MC Version")
                        .errorColor()
                        .addField("Client Version", currentPlayer.getProtocolVersion().getName(), false)
                        .addField("Error", "Client version must be at least 1.20.6", false);
                    return OK;
                }
                currentPlayer.transfer(server.address(), server.port());
                c.getSource().getEmbed()
                    .title("Switched To Server")
                    .addField("Destination", "Name: " + server.name() + "\nAddress: " + server.address() + "\nPort: " + server.port(), false);
                return OK;
            }));
    }

    @Override
    public void defaultEmbed(final Embed embed) {
        var str = CONFIG.server.extra.serverSwitcher.servers.stream()
            .map(s -> s.name() + " -> " + s.address() + ":" + s.port())
            .collect(Collectors.joining("\n"));
        if (str.isBlank()) str = "None";
        embed
            .primaryColor()
            .description("**Registered Servers**\n\n" + str + "\n");
    }
}
