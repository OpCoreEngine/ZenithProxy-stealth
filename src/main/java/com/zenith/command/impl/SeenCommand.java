package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.api.vcapi.VcApi;
import net.dv8tion.jda.api.utils.TimeFormat;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class SeenCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "seen",
            CommandCategory.INFO,
            """
            Gets the first and last times a player was seen on 2b2t using https://api.2b2t.vc
            """,
            asList(
                "<playerName>"
            ),
            asList("firstseen", "lastseen")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("seen")
            .then(argument("playerName", wordWithChars()).executes(c -> {
                final String playerName = getString(c, "playerName");
                var seenResponse = VcApi.INSTANCE.getSeen(playerName);
                if (seenResponse.isEmpty()) {
                    c.getSource().getEmbed()
                        .title(escape(playerName) + " not found")
                        .errorColor();
                    return ERROR;
                }
                c.getSource().getEmbed()
                    .title("Seen")
                    .primaryColor();
                seenResponse.ifPresent((response) -> c.getSource().getEmbed()
                    .addField("Player", playerName, true)
                    .addField("\u200B", "\u200B", true)
                    .addField("\u200B", "\u200B", true)
                    .addField("First Seen", getSeenString(response.firstSeen()), false)
                    .addField("Last Seen", getSeenString(response.lastSeen()), false)
                    .thumbnail(Proxy.getInstance().getAvatarURL(playerName).toString()));

                return 1;
            }));
    }

    private String getSeenString(@Nullable final OffsetDateTime time) {
        if (time == null) return "Never";
        return TimeFormat.DATE_TIME_SHORT.format(time);
    }
}
