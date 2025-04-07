package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.ReplayMod;
import com.zenith.util.Config.Client.Extra.ReplayMod.AutoRecordMode;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class ReplayCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("replay")
            .category(CommandCategory.MODULE)
            .description("""
            Captures a ReplayMod recording.
            
            Replays can optionally be uploaded to discord if they are under the discord message size limit.
            
            If a replay is too large for discord, it can be uploaded to https://file.io instead if `fileIoUpload` is enabled. 
            
            A `maxRecordingTime` of 0 means there is no limit, however, recording are always stopped on disconnects.
            
            `autoStart` will automatically start a new recording when the proxy connects.
            
            Additional recording modes can be configured in the `visualRange` command.
            """)
            .usageLines(
                "start",
                "stop",
                "discordUpload on/off",
                "fileIoUpload on/off",
                "maxRecordingTime <minutes>",
                "autoRecord mode <off/proxyConnected/playerConnected/health>",
                "autoRecord health <integer>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("replay")
            .then(literal("start").executes(c -> {
                var module = MODULE.get(ReplayMod.class);
                if (module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("ReplayMod is already recording");
                    return OK;
                }
                module.enable();
                c.getSource().setNoOutput(true);
                return OK;
            }))
            .then(literal("stop").executes(c -> {
                var module = MODULE.get(ReplayMod.class);
                if (!module.isEnabled()) {
                    c.getSource().getEmbed()
                        .title("Error")
                        .errorColor()
                        .description("ReplayMod is not recording");
                    return OK;
                }
                module.disable();
                c.getSource().setNoOutput(true);
                return OK;
            }))
            .then(literal("discordUpload").requires(Command::validateAccountOwner).then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.replayMod.sendRecordingsToDiscord = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Discord Upload " + toggleStrCaps(CONFIG.client.extra.replayMod.sendRecordingsToDiscord));
                return OK;
            })))
            .then(literal("fileIoUpload").requires(Command::validateAccountOwner).then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.replayMod.fileIOUploadIfTooLarge = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("file.io Upload " + toggleStrCaps(CONFIG.client.extra.replayMod.fileIOUploadIfTooLarge));
                return OK;
            })))
            .then(literal("maxRecordingTime").then(argument("minutes", integer(0, 60 * 6)).executes(c -> {
                CONFIG.client.extra.replayMod.maxRecordingTimeMins = getInteger(c, "minutes");
                c.getSource().getEmbed()
                    .title("Max Recording Time Set");
                return OK;
            })))
            .then(literal("autoRecord")
                      .then(literal("mode").then(argument("mode", enumStrings(AutoRecordMode.values())).executes(c -> {
                          var modeStr = getString(c, "mode").toLowerCase();
                          var foundMode = Arrays.stream(AutoRecordMode.values())
                              .filter(mode -> mode.getName().toLowerCase().equals(modeStr))
                              .findFirst();
                          if (foundMode.isEmpty()) {
                              c.getSource().getEmbed()
                                  .title("Invalid Mode")
                                  .description("Available Modes: " + Arrays.toString(AutoRecordMode.values()));
                              return OK;
                          } else {
                              MODULE.get(ReplayMod.class).disable();
                              CONFIG.client.extra.replayMod.autoRecordMode = foundMode.get();
                              c.getSource().getEmbed()
                                  .title("Auto Record Mode Set");
                          }
                          return OK;
                      })))
                      .then(literal("health").then(argument("health", integer(0, 20)).executes(c -> {
                          CONFIG.client.extra.replayMod.replayRecordingHealthThreshold = getInteger(c, "health");
                          c.getSource().getEmbed()
                              .title("Auto Record Health Set");
                          return OK;
                      }))));
    }

    @Override
    public void postPopulate(final Embed embed) {
        embed
            .primaryColor()
            .addField("Discord Upload", toggleStr(CONFIG.client.extra.replayMod.sendRecordingsToDiscord), false)
            .addField("file.io Upload", toggleStr(CONFIG.client.extra.replayMod.fileIOUploadIfTooLarge), false)
            .addField("Max Recording Time", getMaxRecordingTimeStr(), false)
            .addField("Auto Record Mode", CONFIG.client.extra.replayMod.autoRecordMode.getName(), false);
    }

    private String getMaxRecordingTimeStr() {
        if (CONFIG.client.extra.replayMod.maxRecordingTimeMins <= 0) {
            return "No Limit";
        } else {
            return CONFIG.client.extra.replayMod.maxRecordingTimeMins + " minutes";
        }
    }
}
